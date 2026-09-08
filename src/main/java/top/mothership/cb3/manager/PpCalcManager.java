package top.mothership.cb3.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.mothership.cb3.config.AppProperties;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;
import top.mothership.cb3.pojo.osu.ppcalc.PpCalcRequest;
import top.mothership.cb3.pojo.osu.ppcalc.PpCalcResult;
import top.mothership.cb3.pojo.osu.ppcalc.PpUserScore;
import top.mothership.cb3.util.ApiV2ModeHolder;

import java.util.Arrays;
import java.util.List;

/**
 * rosu-pp-js-http PP计算服务客户端
 * 参考 cabbageWeb 的 ScoreUtil#calcPP 对接方式
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PpCalcManager {

    private static final String PP_CALC_PATH = "/api/PPCalc/CalculateByBeatmapId";

    private final RestTemplate restTemplate;
    private final AppProperties properties;

    /**
     * 调用PP计算服务计算指定成绩的PP
     *
     * @param score     成绩
     * @param userScore 模拟成绩（可由buildActual/buildFc/buildSs/buildAcc构建）
     * @return 计算失败返回null
     */
    public PpCalcResult calc(ApiV2Score.ScoreLazer score, PpUserScore userScore) {
        if (userScore == null || score.getMaximumStatistics() == null) {
            return null;
        }
        var request = new PpCalcRequest();
        request.setBid(score.getBeatmapId());
        request.setRefresh(false);
        request.setUserScore(userScore);
        try {
            return restTemplate.postForObject(
                    properties.getPpCalcUrl() + PP_CALC_PATH, request, PpCalcResult.class);
        } catch (Exception e) {
            log.warn("调用PP计算服务失败 beatmapId={}", score.getBeatmapId(), e);
            return null;
        }
    }

    /**
     * 用成绩本身的判定数据构建请求（实际PP）
     */
    public PpUserScore buildActual(ApiV2Score.ScoreLazer score) {
        var stats = score.getStatistics();
        if (stats == null) {
            return null;
        }
        var us = base(score);
        us.setCombo(score.getMaxCombo());
        switch (score.getMode()) {
            case ApiV2ModeHolder.OSU -> {
                us.setCount300(stats.getCountGreat());
                us.setCount100(stats.getCountOk());
                us.setCount50(stats.getCountMeh());
                us.setCountMiss(stats.getCountMiss());
                us.setLargeTickHits(stats.getLargeTickHit());
                us.setSmallTickHits(stats.getSmallTickHit());
                us.setSliderEndHits(stats.getSliderTailHit());
            }
            case ApiV2ModeHolder.TAIKO -> {
                us.setCount300(stats.getCountGreat());
                us.setCount100(stats.getCountOk());
                us.setCountMiss(stats.getCountMiss());
            }
            case ApiV2ModeHolder.FRUITS -> {
                us.setCount300(stats.getCountGreat());
                us.setCount100(stats.getCountOk());
                us.setCount50(stats.getCountMeh());
                // droplet miss计入miss，tiny droplet miss走nKatu
                us.setCountMiss(stats.getCountMiss() + stats.getLargeTickMiss());
                us.setNKatu(stats.getSmallTickMiss());
            }
            case ApiV2ModeHolder.MANIA -> {
                us.setNGeki(stats.getCountGeki());
                us.setCount300(stats.getCountGreat());
                us.setNKatu(stats.getCountKatu());
                us.setCount100(stats.getCountOk());
                us.setCount50(stats.getCountMeh());
                us.setCountMiss(stats.getCountMiss());
            }
        }
        return us;
    }

    /**
     * 模拟FC成绩：保持100/50（以及mania的320/200）不变，miss并入300，连击取满
     */
    public PpUserScore buildFc(ApiV2Score.ScoreLazer score, long maxCombo) {
        var stats = score.getStatistics();
        if (stats == null) {
            return null;
        }
        var max = score.getMaximumStatistics();
        long objects = objects(score);
        var us = base(score);
        us.setCombo(maxCombo);
        switch (score.getMode()) {
            case ApiV2ModeHolder.OSU -> {
                us.setCount300(objects - stats.getCountOk() - stats.getCountMeh());
                us.setCount100(stats.getCountOk());
                us.setCount50(stats.getCountMeh());
                us.setCountMiss(0L);
                us.setLargeTickHits(max.getLargeTickHit());
                us.setSmallTickHits(max.getSmallTickHit());
                us.setSliderEndHits(max.getSliderTailHit());
            }
            case ApiV2ModeHolder.TAIKO -> {
                us.setCount300(objects - stats.getCountOk());
                us.setCount100(stats.getCountOk());
                us.setCountMiss(0L);
            }
            case ApiV2ModeHolder.FRUITS -> {
                // catch的判定全在miss上，保留已获得的判定，miss清零
                us.setCount300(stats.getCountGreat());
                us.setCount100(stats.getCountOk());
                us.setCount50(stats.getCountMeh());
                us.setCountMiss(0L);
                us.setNKatu(0L);
            }
            case ApiV2ModeHolder.MANIA -> {
                us.setNGeki(stats.getCountGeki());
                us.setCount300(objects - stats.getCountGeki() - stats.getCountKatu()
                        - stats.getCountOk() - stats.getCountMeh());
                us.setNKatu(stats.getCountKatu());
                us.setCount100(stats.getCountOk());
                us.setCount50(stats.getCountMeh());
                us.setCountMiss(0L);
            }
        }
        return us;
    }

    /**
     * 模拟SS成绩：全300（mania为全320），连击取满
     */
    public PpUserScore buildSs(ApiV2Score.ScoreLazer score, long maxCombo) {
        long objects = objects(score);
        var us = base(score);
        us.setCombo(maxCombo);
        switch (score.getMode()) {
            case ApiV2ModeHolder.OSU -> {
                var max = score.getMaximumStatistics();
                us.setCount300(objects);
                us.setCount100(0L);
                us.setCount50(0L);
                us.setCountMiss(0L);
                us.setLargeTickHits(max.getLargeTickHit());
                us.setSmallTickHits(max.getSmallTickHit());
                us.setSliderEndHits(max.getSliderTailHit());
            }
            case ApiV2ModeHolder.TAIKO -> {
                us.setCount300(objects);
                us.setCount100(0L);
                us.setCountMiss(0L);
            }
            case ApiV2ModeHolder.FRUITS -> {
                var max = score.getMaximumStatistics();
                us.setCount300(max.getCountGreat());
                us.setCount100(max.getCountOk());
                us.setCount50(max.getCountMeh());
                us.setCountMiss(0L);
                us.setNKatu(0L);
            }
            case ApiV2ModeHolder.MANIA -> {
                us.setNGeki(objects);
                us.setCount300(0L);
                us.setNKatu(0L);
                us.setCount100(0L);
                us.setCount50(0L);
                us.setCountMiss(0L);
            }
        }
        return us;
    }

    /**
     * 模拟指定acc的FC成绩：只降低一档判定来逼近目标acc，连击取满
     * osu/mania用100(200)折算，taiko用100折算，catch用miss折算
     *
     * @param targetAcc 目标acc，如0.98
     */
    public PpUserScore buildAcc(ApiV2Score.ScoreLazer score, long maxCombo, double targetAcc) {
        long objects = objects(score);
        var us = base(score);
        us.setCombo(maxCombo);
        switch (score.getMode()) {
            case ApiV2ModeHolder.OSU -> {
                var max = score.getMaximumStatistics();
                long n100 = Math.round(objects * (1 - targetAcc) * 1.5);
                us.setCount300(objects - n100);
                us.setCount100(n100);
                us.setCount50(0L);
                us.setCountMiss(0L);
                us.setLargeTickHits(max.getLargeTickHit());
                us.setSmallTickHits(max.getSmallTickHit());
                us.setSliderEndHits(max.getSliderTailHit());
            }
            case ApiV2ModeHolder.TAIKO -> {
                long n100 = Math.round(objects * (1 - targetAcc) * 2);
                us.setCount300(objects - n100);
                us.setCount100(n100);
                us.setCountMiss(0L);
            }
            case ApiV2ModeHolder.FRUITS -> {
                var max = score.getMaximumStatistics();
                long miss = Math.round(objects * (1 - targetAcc));
                us.setCount300(max.getCountGreat() - miss);
                us.setCount100(max.getCountOk());
                us.setCount50(max.getCountMeh());
                us.setCountMiss(miss);
                us.setNKatu(0L);
            }
            case ApiV2ModeHolder.MANIA -> {
                long nKatu = Math.round(objects * (1 - targetAcc) * 1.5);
                us.setCount300(objects - nKatu);
                us.setNKatu(nKatu);
                us.setNGeki(0L);
                us.setCount100(0L);
                us.setCount50(0L);
                us.setCountMiss(0L);
            }
        }
        return us;
    }

    /**
     * 各模式的物量：osu/taiko/mania即最大300数，catch为水果+大滴+小滴
     */
    private long objects(ApiV2Score.ScoreLazer score) {
        var max = score.getMaximumStatistics();
        return switch (score.getMode()) {
            case ApiV2ModeHolder.FRUITS -> max.getCountGreat() + max.getCountOk() + max.getCountMeh();
            default -> max.getCountGreat();
        };
    }

    private PpUserScore base(ApiV2Score.ScoreLazer score) {
        var us = new PpUserScore();
        us.setMods(score.getMods() == null ? List.of() : Arrays.asList(score.getMods()));
        us.setMode(score.getModeInt());
        us.setLazer(true);
        return us;
    }
}
