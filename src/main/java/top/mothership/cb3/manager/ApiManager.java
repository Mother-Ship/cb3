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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ApiManager {
    private static final String API_BASE_URL = "https://osu.ppy.sh/api";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CustomPropertiesConfig customPropertiesConfig;

    public ApiV1UserInfoVO getUserInfo(Integer mode, String username) {
        String result = accessAPI("get_user", "u", username, "type", "string", "m", mode.toString());
        return parseResponse(result, ApiV1UserInfoVO.class);
    }

    public ApiV1UserInfoVO getUserInfo(Integer mode, Integer userId) {
        String result = accessAPI("get_user", "u", userId.toString(), "type", "id", "m", mode.toString());
        return parseResponse(result, ApiV1UserInfoVO.class);
    }

    public ApiV1BeatmapInfoVO getBeatmap(Integer bid) {
        String result = accessAPI("get_beatmaps", "b", bid.toString());
        return parseResponse(result, new TypeReference<List<ApiV1BeatmapInfoVO>>() {}).get(0);
    }

    public List<ApiV1BeatmapInfoVO> getBeatmaps(Integer sid) {
        String result = accessAPI("get_beatmaps", "s", sid.toString());
        return parseResponse(result, new TypeReference<>() {
        });
    }

    public ApiV1BeatmapInfoVO getBeatmap(String hash) {
        String result = accessAPI("get_beatmaps", "h", hash);
        return parseResponse(result, new TypeReference<List<ApiV1BeatmapInfoVO>>() {}).get(0);
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
            List<ApiV1UserBestScoreVO> list = parseResponse(result, new TypeReference<List<ApiV1UserBestScoreVO>>() {});
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
        return parseResponse(result, new TypeReference<List<ApiV1UserRecentScoreVO>>() {}).get(0);
    }

    public ApiV1UserRecentScoreVO getRecent(Integer mode, Integer userId) {
        String result = accessAPI("get_user_recent", "u", userId.toString(), "type", "id", "m", mode.toString());
        return parseResponse(result, new TypeReference<List<ApiV1UserRecentScoreVO>>() {}).get(0);
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
            return objectMapper.readValue("[" + response + "]", typeReference);
        } catch (IOException e) {
            log.error("Error parsing response", e);
            return null;
        }
    }

    private <T> T parseResponse(String response, Class<T> clazz) {
        try {
            return objectMapper.readValue("[" + response + "]", clazz);
        } catch (IOException e) {
            log.error("Error parsing response", e);
            return null;
        }
    }

    private String accessAPI(String endpoint, Object... params) {
        String url = API_BASE_URL + "/" + endpoint;
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("k", customPropertiesConfig.getApikey());

        for (int i = 0; i < params.length; i += 2) {
            urlBuilder.addQueryParameter(params[i].toString(), URLEncoder.encode(params[i + 1].toString(), StandardCharsets.UTF_8));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Request failed: {}", response.message());
                return null;
            }
            return response.body().string();
        } catch (IOException e) {
            log.error("Error making request", e);
            return null;
        }
    }
}
