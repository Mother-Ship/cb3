package top.mothership.cb3.pojo.osu.ppcalc;

import lombok.Data;

/**
 * PP计算服务请求体
 * 对应 rosu-pp-js-http 的 POST /api/PPCalc/CalculateByBeatmapId
 */
@Data
public class PpCalcRequest {

    /**
     * 谱面ID
     */
    private long bid;

    /**
     * 是否强制重新下载谱面文件
     */
    private boolean refresh;

    /**
     * 模拟成绩
     */
    private PpUserScore userScore;
}
