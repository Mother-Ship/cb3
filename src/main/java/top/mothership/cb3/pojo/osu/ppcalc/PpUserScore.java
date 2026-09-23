package top.mothership.cb3.pojo.osu.ppcalc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;

import java.util.List;

/**
 * PP计算服务使用的成绩数据
 * 对应 rosu-pp-js-http 的 userScore 字段
 *
 * <p>字段名必须显式声明：全局 ObjectMapper 配了 SNAKE_CASE，
 * 不加注解 largeTickHits 之类会变成 large_tick_hits，服务端取不到值。</p>
 */
@Data
public class PpUserScore {

    /**
     * mods，acronym+settings 的序列化结果与osu api v2返回一致，rosu-pp-js可直接解析
     */
    @JsonProperty("mods")
    private List<ApiV2Score.Mod> mods;

    /**
     * 连击，mania无效
     */
    @JsonProperty("combo")
    private Long combo;

    /**
     * 模式（0 osu / 1 taiko / 2 fruits / 3 mania）
     */
    @JsonProperty("mode")
    private Integer mode;

    /**
     * 是否lazer成绩
     */
    @JsonProperty("lazer")
    private Boolean lazer;

    @JsonProperty("count300")
    private Long count300;
    @JsonProperty("count100")
    private Long count100;
    @JsonProperty("count50")
    private Long count50;
    @JsonProperty("countMiss")
    private Long countMiss;

    /**
     * lazer专属判定（stable会被忽略）
     */
    @JsonProperty("largeTickHits")
    private Long largeTickHits;
    @JsonProperty("smallTickHits")
    private Long smallTickHits;
    @JsonProperty("sliderEndHits")
    private Long sliderEndHits;

    /**
     * mania的n320
     */
    @JsonProperty("nGeki")
    private Long nGeki;

    /**
     * catch的tiny droplet miss / mania的n200
     */
    @JsonProperty("nKatu")
    private Long nKatu;
}
