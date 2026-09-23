package top.mothership.cb3.pojo.osu.ppcalc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * PP计算服务请求体
 * 对应 rosu-pp-js-http 的 POST /api/PPCalc/CalculateByBeatmapId
 *
 * <p>字段名必须显式声明：全局 ObjectMapper 配了 SNAKE_CASE，
 * 不加注解 userScore 会被序列化成 user_score，服务端取不到会返回
 * 400 "Missing required parameters: bid and userScore"。</p>
 */
@Data
public class PpCalcRequest {

    /**
     * 谱面ID
     */
    @JsonProperty("bid")
    private long bid;

    /**
     * 是否强制重新下载谱面文件
     */
    @JsonProperty("refresh")
    private boolean refresh;

    /**
     * 模拟成绩
     */
    @JsonProperty("userScore")
    private PpUserScore userScore;
}
