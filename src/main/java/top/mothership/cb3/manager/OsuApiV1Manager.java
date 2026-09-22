package top.mothership.cb3.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cb3.config.CustomPropertiesConfig;
import top.mothership.cb3.pojo.osu.apiv1.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * osu! API v1（旧接口 /api/**）客户端。
 *
 * <p>注意：v1 超限时会被 Cloudflare 以 {@code HTTP 429 + "error code: 1015"} 拦下，
 * 因此所有请求都先经过 {@link OsuRateLimiter#BUCKET_V1} 排队，并在收到 429 时按
 * {@code Retry-After} 暂停 + 降速。</p>
 *
 * <p>等待策略：交互式调用（QQ 命令）最多等 {@value #INTERACTIVE_MAX_WAIT_SECONDS} 秒，
 * 超出就快速失败；录入任务用 {@link #getUserInfoForImport(Integer, Integer)}，会老实等满 Retry-After。</p>
 */
@Component
@Slf4j
public class OsuApiV1Manager {
    private static final String API_BASE_URL = "https://osu.ppy.sh/api";

    /**
     * 默认的 OkHttpClient 没有超时设置，一旦连接卡住线程会一直占着，
     * 这里补上超时；同时带上 User-Agent（Cloudflare 对无 UA 的高频请求更容易拦截）。
     */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(20))
            .writeTimeout(Duration.ofSeconds(20))
            .callTimeout(Duration.ofSeconds(60))
            .build();

    private static final String USER_AGENT = "cb3/0.0.1 (+https://github.com/Mother-Ship/cb3)";

    /** 单次请求内部最多尝试次数（含首次） */
    private static final int MAX_ATTEMPTS = 3;
    /** 没有 Retry-After 时的默认退避时长 */
    private static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(5);
    /** 单次退避上限。实测 Cloudflare 的 Retry-After 最高给到 ~1800s，必须等满，
     * 否则提前重试反而会延长封禁 */
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);
    /** 交互式命令允许的最大排队等待，超过就降级为立即失败，不让用户干等半小时 */
    private static final long INTERACTIVE_MAX_WAIT_SECONDS = 3;
    private static final Duration INTERACTIVE_MAX_WAIT = Duration.ofSeconds(INTERACTIVE_MAX_WAIT_SECONDS);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CustomPropertiesConfig customPropertiesConfig;
    @Autowired
    private OsuRateLimiter osuRateLimiter;

    public ApiV1UserInfoVO getUserInfo(Integer mode, String username) {
        String result = accessAPI("get_user", "u", username, "type", "string", "m", mode.toString());
        List<ApiV1UserInfoVO> responseList = parseResponse(result, new TypeReference<>() {
        });
        if (responseList != null && !responseList.isEmpty()) {
            return responseList.get(0);
        }
        log.error("get_user接口查询用户失败: {} 模式：{} 返回：{}", username, mode, result);
        return null;
    }

    /**
     * 交互式查询：最多等 {@value #INTERACTIVE_MAX_WAIT_SECONDS} 秒，超时快速失败。
     */
    public ApiV1UserInfoVO getUserInfo(Integer mode, Integer userId) {
        return getUserInfo(mode, userId, INTERACTIVE_MAX_WAIT);
    }

    /**
     * 录入任务专用：批量任务没有用户在等，允许一直排队等满 Retry-After。
     */
    public ApiV1UserInfoVO getUserInfoForImport(Integer mode, Integer userId) {
        return getUserInfo(mode, userId, null);
    }

    private ApiV1UserInfoVO getUserInfo(Integer mode, Integer userId, Duration maxWait) {
        String result = accessAPI(maxWait, "get_user", "u", userId.toString(), "type", "id", "m", mode.toString());
        List<ApiV1UserInfoVO> responseList = parseResponse(result, new TypeReference<List<ApiV1UserInfoVO>>() {
        });
        if (responseList != null && !responseList.isEmpty()) {
            return responseList.get(0);
        }

        log.error("get_user接口查询用户失败: {} 模式：{} 返回：{}", userId, mode, result);
        return null;
    }

    public ApiV1BeatmapInfoVO getBeatmap(Integer bid) {
        String result = accessAPI("get_beatmaps", "b", bid.toString());
        List<ApiV1BeatmapInfoVO> responseList = parseResponse(result, new TypeReference<List<ApiV1BeatmapInfoVO>>() {
        });
        if (responseList != null && !responseList.isEmpty()) {
            return responseList.get(0);
        }
        log.error("get_beatmaps接口查询谱面失败: {} 返回：{}", bid, result);
        return null;
    }

    public List<ApiV1BeatmapInfoVO> getBeatmaps(Integer sid) {
        String result = accessAPI("get_beatmaps", "s", sid.toString());
        return parseResponse(result, new TypeReference<>() {
        });
    }

    public ApiV1BeatmapInfoVO getBeatmap(String hash) {
        String result = accessAPI("get_beatmaps", "h", hash);
        return parseResponse(result, new TypeReference<List<ApiV1BeatmapInfoVO>>() {
        }).get(0);
    }

    public List<ApiV1UserBestScoreVO> getBP(Integer mode, String username) {
        String result = accessAPI("get_user_best", "u", username, "type", "string", "m", mode.toString());
        List<ApiV1UserBestScoreVO> list = parseResponse(result, new TypeReference<>() {
        });
        for (ApiV1UserBestScoreVO s : list) {
            s.setMode(mode.byteValue());
        }
        return list;
    }

    public List<ApiV1UserBestScoreVO> getBP(Integer mode, Integer userId) {
        String result = accessAPI("get_user_best", "u", userId.toString(), "type", "id", "m", mode.toString());
        List<ApiV1UserBestScoreVO> list = parseResponse(result, new TypeReference<>() {
        });
        for (ApiV1UserBestScoreVO s : list) {
            s.setMode(mode.byteValue());
        }
        return list;
    }

    public List<ApiV1UserBestScoreVO> getBP(String username) {
        List<ApiV1UserBestScoreVO> resultList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String result = accessAPI("get_user_best", "u", username, "type", "string", "m", String.valueOf(i));
            List<ApiV1UserBestScoreVO> list = parseResponse(result, new TypeReference<List<ApiV1UserBestScoreVO>>() {
            });
            for (ApiV1UserBestScoreVO s : list) {
                s.setMode((byte) i);
            }
            resultList.addAll(list);
        }
        return resultList;
    }

    public List<List<ApiV1UserBestScoreVO>> getBP(Integer userId) {
        List<List<ApiV1UserBestScoreVO>> resultList = new ArrayList<>();
        //小技巧，这里i设为byte
        for (int i = 0; i < 4; i++) {
            String result = accessAPI("get_user_best", "u", userId.toString(), "type", "id", "m", String.valueOf(i));
            List<ApiV1UserBestScoreVO> list = parseResponse(result, new TypeReference<>() {
            });
            for (ApiV1UserBestScoreVO s : list) {
                s.setMode((byte) i);
            }
            resultList.add(list);
        }
        return resultList;
    }

    public ApiV1UserRecentScoreVO getRecent(Integer mode, String username) {
        String result = accessAPI("get_user_recent", "u", username, "type", "string", "m", mode.toString());
        return parseResponse(result, new TypeReference<List<ApiV1UserRecentScoreVO>>() {
        }).get(0);
    }

    public ApiV1UserRecentScoreVO getRecent(Integer mode, Integer userId) {
        String result = accessAPI("get_user_recent", "u", userId.toString(), "type", "id", "m", mode.toString());
        return parseResponse(result, new TypeReference<List<ApiV1UserRecentScoreVO>>() {
        }).get(0);
    }

    // 用于获取所有的recent
    public List<ApiV1UserRecentScoreVO> getRecents(Integer mode, String username) {
        String result = accessAPI("get_user_recent", "u", username, "type", "string", "m", mode.toString());
        return parseResponse(result, new TypeReference<>() {
        });
    }

    public List<ApiV1UserRecentScoreVO> getRecents(Integer mode, Integer userId) {
        String result = accessAPI("get_user_recent", "u", userId.toString(), "type", "id", "m", mode.toString());
        return parseResponse(result, new TypeReference<>() {
        });
    }

    public List<ApiV1BeatmapScoreVO> getFirstScore(Integer mode, Integer bid, Integer rank) {
        String result = accessAPI("get_scores", "b", bid.toString(), "limit", rank.toString(), "m", mode.toString());
        return parseResponse(result, new TypeReference<>() {
        });
    }

    public List<ApiV1BeatmapScoreVO> getScore(Integer mode, Integer bid, Integer uid) {
        String result = accessAPI("get_scores", "u", uid.toString(), "b", bid.toString(), "m", mode.toString());
        return parseResponse(result, new TypeReference<>() {
        });
    }

    private <T> T parseResponse(String response, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(response, typeReference);
        } catch (IOException e) {
            // 解析失败说明接口返回了非预期内容，属于“接口不可用”，不能当成“用户不存在/被封禁”
            throw new OsuApiUnavailableException("解析 osu! API v1 响应失败", e);
        }
    }

    /**
     * 交互式请求的默认入口：最多等 {@value #INTERACTIVE_MAX_WAIT_SECONDS} 秒。
     */
    private String accessAPI(String endpoint, Object... params) {
        return accessAPI(INTERACTIVE_MAX_WAIT, endpoint, params);
    }

    /**
     * 发起一次 v1 请求。
     *
     * <p>成功时返回响应体；空数组是合法结果（调用方据此判断用户不存在/被封禁）。
     * 其余情况（429 限流、5xx、网络异常、鉴权失败等）一律抛出
     * {@link OsuApiUnavailableException}，避免被上层误判为玩家被封禁。</p>
     *
     * @param maxWait 排队等待上限；{@code null} 表示不设上限（批量任务用）
     */
    private String accessAPI(Duration maxWait, String endpoint, Object... params) {
        String url = API_BASE_URL + "/" + endpoint;
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("k", customPropertiesConfig.getApikey());

        for (int i = 0; i < params.length; i += 2) {
            // addQueryParameter 内部会做 URL 编码，这里不能再手动 encode 一次（否则会双重编码）
            urlBuilder.addQueryParameter(params[i].toString(), params[i + 1].toString());
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", USER_AGENT)
                .build();

        OsuApiUnavailableException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 所有 v1 请求统一排队；交互式请求等待超限时会在这里直接抛异常
            osuRateLimiter.acquire(OsuRateLimiter.BUCKET_V1, maxWait);
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return response.body() == null ? "" : response.body().string();
                }

                int code = response.code();
                if (code == 429) {
                    Duration backoff = backoffFrom(response);
                    // 按 Retry-After 暂停整个桶，并把速率降一档
                    osuRateLimiter.onRateLimited(OsuRateLimiter.BUCKET_V1, backoff);
                    if (attempt < MAX_ATTEMPTS && canWait(maxWait, backoff)) {
                        log.warn("osu! API v1 {} 触发限流(429)，第 {}/{} 次尝试，按 Retry-After 退避 {} 秒后重试",
                                endpoint, attempt, MAX_ATTEMPTS, backoff.toSeconds());
                        sleep(backoff);
                        continue;
                    }
                    lastError = new OsuApiUnavailableException("osu! API v1 " + endpoint
                            + " 被限流(429)，Retry-After=" + backoff.toSeconds()
                            + "s，超出本次允许等待 " + describeMaxWait(maxWait));
                    break;
                }

                // 5xx 是服务端暂时故障，可以重试；其余 4xx（如 key 失效）属于配置问题，
                // 同样不能当作“用户不存在”，直接抛出
                if (code >= 500 && attempt < MAX_ATTEMPTS) {
                    log.warn("osu! API v1 {} 返回 {}，第 {}/{} 次尝试，{} 秒后重试",
                            endpoint, code, attempt, MAX_ATTEMPTS, DEFAULT_BACKOFF.toSeconds());
                    sleep(DEFAULT_BACKOFF);
                    continue;
                }
                lastError = new OsuApiUnavailableException("osu! API v1 " + endpoint + " 返回 HTTP " + code);
                break;
            } catch (IOException e) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("osu! API v1 {} 请求异常（第 {}/{} 次尝试）：{}",
                            endpoint, attempt, MAX_ATTEMPTS, e.getMessage());
                    sleep(DEFAULT_BACKOFF);
                    continue;
                }
                lastError = new OsuApiUnavailableException("osu! API v1 " + endpoint + " 请求异常", e);
                break;
            }
        }

        if (lastError == null) {
            lastError = new OsuApiUnavailableException("osu! API v1 " + endpoint + " 请求失败");
        }
        throw lastError;
    }

    /**
     * 是否值得按 Retry-After 等下去：批量任务（maxWait=null）永远等；
     * 交互式请求只在 Retry-After 不超过上限时才等，否则立即失败。
     */
    private boolean canWait(Duration maxWait, Duration backoff) {
        return maxWait == null || backoff.compareTo(maxWait) <= 0;
    }

    private String describeMaxWait(Duration maxWait) {
        return maxWait == null ? "（无上限）" : maxWait.toSeconds() + " 秒";
    }

    /**
     * 解析 429 的 Retry-After。Cloudflare 实测会给出 100s 甚至 1800s 级别的退避时间。
     */
    private Duration backoffFrom(Response response) {
        String retryAfter = response.header("Retry-After");
        if (retryAfter != null) {
            try {
                long seconds = Long.parseLong(retryAfter.trim());
                if (seconds > 0) {
                    return Duration.ofSeconds(Math.min(seconds, MAX_BACKOFF.toSeconds()));
                }
            } catch (NumberFormatException e) {
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
