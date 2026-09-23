package top.mothership.cb3.pojo.osu.ppcalc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * PP计算服务响应体
 *
 * <p>字段名必须显式声明：全局 ObjectMapper 配了 SNAKE_CASE，
 * 不加注解会去匹配 score_result / beatmap_info，服务端返回的
 * scoreResult / beatmapInfo 就全部解析为 null。</p>
 */
@Data
public class PpCalcResult {

    @JsonProperty("scoreResult")
    private PpScoreResult scoreResult;

    @JsonProperty("beatmapInfo")
    private PpBeatmapInfo beatmapInfo;
}
