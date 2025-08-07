package top.mothership.cb3.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import top.mothership.cb3.config.CustomPropertiesConfig;
import top.mothership.cb3.pojo.osu.apiv2.OAuthCredentials;
import top.mothership.cb3.pojo.osu.apiv2.request.UserScoresRequest;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;
import top.mothership.cb3.pojo.osu.apiv2.response.TokenResponse;

import java.time.LocalDateTime;
import java.util.List;
@Component
@Slf4j
public class OsuApiV2Manager {

    private static final String OSU_TOKEN_URL = "https://osu.ppy.sh/oauth/token";
    private static final String OSU_API_BASE_URL = "https://osu.ppy.sh/api/v2";

    @Autowired
    CustomPropertiesConfig propertiesConfig;
    @Autowired
    private RestTemplate restTemplate;


    private final OAuthCredentials credentials = new OAuthCredentials();

    private void updateCredentials(TokenResponse tokenResponse) {
        credentials.setAccessToken(tokenResponse.getAccessToken());
        credentials.setExpiresIn(tokenResponse.getExpiresIn());
        credentials.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 刷新访问令牌
     */
    public void refreshAccessToken() {


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", propertiesConfig.getApiV2Id());
        requestBody.add("client_secret", propertiesConfig.getApiV2Secret());
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("scope", "public");
        requestBody.add("code", "cabbage");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);


        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                OSU_TOKEN_URL, request, TokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            TokenResponse tokenResponse = response.getBody();
            updateCredentials(tokenResponse);
            log.info("更新API V2 Token成功, result: {}", tokenResponse);
        } else {
            log.error("更新API V2 Token 失败: {}", response.getStatusCode());
        }

    }


    /**
     * 获取有效的访问令牌
     */
    public String getValidAccessToken() {
        if (credentials == null) {
            throw new IllegalStateException("OAuth credentials not initialized");
        }

        if (credentials.isTokenExpired()) {
            refreshAccessToken();
        }
        log.info("获取API V2 Access Token成功：{}",credentials.getAccessToken());
        return credentials.getAccessToken();
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
        log.info("获取用户成绩，拼接的URL：{}", url);

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("x-api-version", "20220705");


        HttpEntity<?> entity = new HttpEntity<>(headers);


        try {
            ResponseEntity<List<ApiV2Score.ScoreLazer>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<ApiV2Score.ScoreLazer> scores = response.getBody();
                log.info("成功获取 {} 条用户{}类型的成绩",
                        scores != null ? scores.size() : 0, scoreType);
                return scores;
            } else {
                log.error("获取用户成绩失败: {}", response.getStatusCode());
                throw new RuntimeException("从osu! API获取用户成绩失败");
            }
        } catch (Exception e) {
            System.out.println("发送HTTP请求" + url +  entity);
            log.error("获取用户成绩时发生错误: ", e);
            throw new RuntimeException("从osu! API获取用户成绩时发生错误", e);
        }
    }

}
