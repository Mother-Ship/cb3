package top.mothership.cb3.pojo.osu.ppcalc;

import lombok.Data;

/**
 * PP计算服务响应体
 */
@Data
public class PpCalcResult {

    private PpScoreResult scoreResult;

    private PpBeatmapInfo beatmapInfo;
}
