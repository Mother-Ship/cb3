package top.mothership.cb3.pojo.osu.ppcalc;

import lombok.Data;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;

import java.util.List;

/**
 * PP计算服务使用的成绩数据
 * 对应 rosu-pp-js-http 的 userScore 字段
 */
@Data
public class PpUserScore {

    /**
     * mods，acronym+settings 的序列化结果与osu api v2返回一致，rosu-pp-js可直接解析
     */
    private List<ApiV2Score.Mod> mods;

    /**
     * 连击，mania无效
     */
    private Long combo;

    /**
     * 模式（0 osu / 1 taiko / 2 fruits / 3 mania）
     */
    private Integer mode;

    /**
     * 是否lazer成绩
     */
    private Boolean lazer;

    private Long count300;
    private Long count100;
    private Long count50;
    private Long countMiss;

    /**
     * lazer专属判定（stable会被忽略）
     */
    private Long largeTickHits;
    private Long smallTickHits;
    private Long sliderEndHits;

    /**
     * mania的n320
     */
    private Long nGeki;

    /**
     * catch的tiny droplet miss / mania的n200
     */
    private Long nKatu;
}
