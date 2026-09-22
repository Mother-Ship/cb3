package top.mothership.cb3.manager;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import top.mothership.cb3.config.CustomPropertiesConfig;
import top.mothership.cb3.pojo.osu.apiv2.OAuthCredentials;
import top.mothership.cb3.pojo.osu.apiv2.request.UserScoresRequest;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2User;
import top.mothership.cb3.pojo.osu.apiv2.response.TokenResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * osu! API v2 客户端。
 *
 * <p>实测线上限流：数据接口 {@code x-ratelimit-limit = 1200}（次/分钟），
 * 令牌接口 {@code x-ratelimit-limit = 60}（次/分钟）。所有请求先经过
 * {@link OsuRateLimiter} 排队；收到 429 时按 {@code Retry-After} 暂停 + 降速，
 * 且等待上限为 {@value #INTERACTIVE_MAX_WAIT_SECONDS} 秒——v2 的调用方都是 QQ 命令，
 * 不能让用户干等半小时，超过上限直接快速失败。</p>
 */
@Component
@Slf4j
public class OsuApiV2Manager {

    private static final String OSU_TOKEN_URL = "https://osu.ppy.sh/oauth/token";
    private static final String OSU_API_BASE_URL = "https://osu.ppy.sh/api/v2";
    /** 单次请求内部最多尝试次数（含首次） */
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(5);
    /** 单次退避上限：实测 Cloudflare 的 Retry-After 最高给到 ~1800s，必须等满 */
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);
    /** 交互式命令允许的最大排队等待，超过就降级为立即失败 */
    private static final long INTERACTIVE_MAX_WAIT_SECONDS = 3;
    private static final Duration INTERACTIVE_MAX_WAIT = Duration.ofSeconds(INTERACTIVE_MAX_WAIT_SECONDS);

    private final OAuthCredentials credentials = new OAuthCredentials();
    @Autowired
    CustomPropertiesConfig propertiesConfig;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private OsuRateLimiter osuRateLimiter;

    private void updateCredentials(TokenResponse tokenResponse) {
        credentials.setAccessToken(tokenResponse.getAccessToken());
        credentials.setExpiresIn(tokenResponse.getExpiresIn());
        credentials.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 刷新访问令牌。
     *
     * <p>用 synchronized + 双重检查避免多个线程同时打 /oauth/token（该接口实测只有 60 次/分钟），
     * 同时不再把令牌内容打进日志。</p>
     */
    public void refreshAccessToken() {
        synchronized (credentials) {
            if (!credentials.isTokenExpired()) {
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", propertiesConfig.getApiV2Id());
            requestBody.add("client_secret", propertiesConfig.getApiV2Secret());
            requestBody.add("grant_type", "client_credentials");
            requestBody.add("scope", "public");
            requestBody.add("code", "cabbage");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                osuRateLimiter.acquire(OsuRateLimiter.BUCKET_TOKEN, INTERACTIVE_MAX_WAIT);
                try {
                    ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                            OSU_TOKEN_URL, request, TokenResponse.class);
                    TokenResponse tokenResponse = response.getBody();
                    if (response.getStatusCode() == HttpStatus.OK && tokenResponse != null) {
                        updateCredentials(tokenResponse);
                        log.info("更新 API V2 Token 成功，有效期 {} 秒", tokenResponse.getExpiresIn());
                        return;
                    }
                    throw new OsuApiUnavailableException("更新 API V2 Token 失败：" + response.getStatusCode());
                } catch (HttpStatusCodeException e) {
                    if (e.getStatusCode().value() == 429) {
                        Duration backoff = backoffFrom(e);
                        osuRateLimiter.onRateLimited(OsuRateLimiter.BUCKET_TOKEN, backoff);
                        if (attempt < MAX_ATTEMPTS && canWait(backoff)) {
                            log.warn("获取 API V2 Token 触发限流(429)，第 {}/{} 次尝试，按 Retry-After 退避 {} 秒",
                                    attempt, MAX_ATTEMPTS, backoff.toSeconds());
                            sleep(backoff);
                            continue;
                        }
                        throw new OsuApiUnavailableException("获取 API V2 Token 被限流(429)，Retry-After="
                                + backoff.toSeconds() + "s，超出本次允许等待 " + INTERACTIVE_MAX_WAIT_SECONDS + " 秒", e);
                    }
                    throw new OsuApiUnavailableException("更新 API V2 Token 失败：" + e.getStatusCode(), e);
                } catch (RestClientException e) {
                    if (attempt < MAX_ATTEMPTS) {
                        log.warn("更新 API V2 Token 请求异常（第 {}/{} 次尝试）：{}",
                                attempt, MAX_ATTEMPTS, e.getMessage());
                        sleep(DEFAULT_BACKOFF);
                        continue;
                    }
                    throw new OsuApiUnavailableException("更新 API V2 Token 请求异常", e);
                }
            }
            throw new OsuApiUnavailableException("更新 API V2 Token 失败");
        }
    }

    /**
     * 获取有效的访问令牌
     */
    public String getValidAccessToken() {
        // 同步返回，保证令牌写入后对其他线程立即可见（字段本身不是 volatile）
        synchronized (credentials) {
            if (credentials.isTokenExpired()) {
                refreshAccessToken();
            }
            return credentials.getAccessToken();
        }
    }

    /**
     * 获取用户最佳成绩
     */
    public List<ApiV2Score.ScoreLazer> getUserBestScores(UserScoresRequest request) {
        return getUserScores(request, "best");
    }

    /**
     * 获取用户最近成绩
     */
    public List<ApiV2Score.ScoreLazer> getUserRecentScores(UserScoresRequest request) {
        return getUserScores(request, "recent");
    }

    /**
     * 获取用户信息
     */
    public ApiV2User.User getUserInfo(String userId) {
        return getUserInfo(null, userId);
    }

    public ApiV2User.User getUserInfo(String mode, String userId) {
        String url = OSU_API_BASE_URL + "/users/" + userId;
        if (mode != null) {
            url += "/" + mode;
        }

        HttpHeaders headers = generateHeaders();

        return executeHttpGet(url, headers, new ParameterizedTypeReference<>() {
        });
    }

    /**
     * 获取用户成绩通用方法
     */
    private List<ApiV2Score.ScoreLazer> getUserScores(UserScoresRequest request, String scoreType) {
        // 构建URL
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(OSU_API_BASE_URL + "/users/" + request.getUserId() + "/scores/" + scoreType);

        // 添加可选参数
        if (request.getLimit() != null) {
            uriBuilder.queryParam("limit", request.getLimit());
        }

        if (request.getOffset() != null) {
            uriBuilder.queryParam("offset", request.getOffset());
        }

        if ("recent".equals(scoreType)) {
            if (request.getIncludeFails() != null) {
                uriBuilder.queryParam("include_fails", request.getIncludeFails() ? "1" : "0");
            }
            if (request.getLegacyOnly() != null) {
                uriBuilder.queryParam("legacy_only", request.getLegacyOnly() ? "1" : "0");
            }
        }

        if (request.getMode() != null) {
            uriBuilder.queryParam("mode", request.getMode());
        }

        String url = uriBuilder.build().toUriString();
        log.debug("获取用户成绩，拼接的URL：{}", url);

        HttpHeaders headers = generateHeaders();

        return executeHttpGet(url, headers, new ParameterizedTypeReference<>() {
        });
    }

    @NotNull
    private HttpHeaders generateHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("x-api-version", "20220705");
        return headers;
    }

    /**
     * 执行HTTP GET请求的通用方法。
     *
     * <p>429 会按 Retry-After 暂停 + 降速；Retry-After 超过交互式等待上限时立即失败。
     * 5xx / 网络异常重试后抛 {@link OsuApiUnavailableException}；
     * 其余 4xx 原样抛出（例如 404 表示用户不存在）。</p>
     */
    private <T> T executeHttpGet(String url, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 所有 v2 请求统一排队限流；等待超限会在这里直接抛异常
            osuRateLimiter.acquire(OsuRateLimiter.BUCKET_V2, INTERACTIVE_MAX_WAIT);
            try {
                ResponseEntity<T> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        responseType
                );
                if (response.getStatusCode() == HttpStatus.OK) {
                    return response.getBody();
                }
                throw new OsuApiUnavailableException("osu! API v2 返回 " + response.getStatusCode());
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == 429) {
                    Duration backoff = backoffFrom(e);
                    // 按 Retry-After 暂停整个桶，并把速率降一档
                    osuRateLimiter.onRateLimited(OsuRateLimiter.BUCKET_V2, backoff);
                    if (attempt < MAX_ATTEMPTS && canWait(backoff)) {
                        log.warn("osu! API v2 触发限流(429)，第 {}/{} 次尝试，按 Retry-After 退避 {} 秒后重试：{}",
                                attempt, MAX_ATTEMPTS, backoff.toSeconds(), url);
                        sleep(backoff);
                        continue;
                    }
                    throw new OsuApiUnavailableException("osu! API v2 被限流(429)，Retry-After="
                            + backoff.toSeconds() + "s，超出本次允许等待 " + INTERACTIVE_MAX_WAIT_SECONDS + " 秒：" + url, e);
                }
                if (status >= 500 && attempt < MAX_ATTEMPTS) {
                    log.warn("osu! API v2 返回 {}，第 {}/{} 次尝试，{} 秒后重试：{}",
                            status, attempt, MAX_ATTEMPTS, DEFAULT_BACKOFF.toSeconds(), url);
                    sleep(DEFAULT_BACKOFF);
                    continue;
                }
                if (status >= 500) {
                    throw new OsuApiUnavailableException("osu! API v2 返回 " + status + "：" + url, e);
                }
                // 其余 4xx（如 404 用户不存在）交由上层判断
                throw e;
            } catch (RestClientException e) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("osu! API v2 请求异常（第 {}/{} 次尝试）：{}", attempt, MAX_ATTEMPTS, e.getMessage());
                    sleep(DEFAULT_BACKOFF);
                    continue;
                }
                throw new OsuApiUnavailableException("osu! API v2 请求异常：" + url, e);
            }
        }
        throw new OsuApiUnavailableException("osu! API v2 请求失败：" + url);
    }

    /**
     * 交互式请求只在 Retry-After 不超过上限时才等，否则立即失败。
     */
    private boolean canWait(Duration backoff) {
        return backoff.compareTo(INTERACTIVE_MAX_WAIT) <= 0;
    }

    private Duration backoffFrom(HttpStatusCodeException e) {
        HttpHeaders responseHeaders = e.getResponseHeaders();
        String retryAfter = responseHeaders == null ? null : responseHeaders.getFirst("Retry-After");
        if (retryAfter != null) {
            try {
                long seconds = Long.parseLong(retryAfter.trim());
                if (seconds > 0) {
                    return Duration.ofSeconds(Math.min(seconds, MAX_BACKOFF.toSeconds()));
                }
            } catch (NumberFormatException ignored) {
                log.warn("无法解析 Retry-After 头：{}", retryAfter);
            }
        }
        return DEFAULT_BACKOFF;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OsuApiUnavailableException("等待 osu! API 重试时被中断", e);
        }
    }
}
