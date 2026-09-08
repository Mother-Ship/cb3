package top.mothership.cb3.pojo.osu.ppcalc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * PP计算服务返回的谱面信息
 */
@Data
public class PpBeatmapInfo {

    @JsonProperty("od")
    private double od;

    @JsonProperty("ar")
    private double ar;

    @JsonProperty("cs")
    private double cs;

    @JsonProperty("hp")
    private double hp;

    @JsonProperty("stars")
    private double stars;

    @JsonProperty("speedStars")
    private Double speedStars;

    @JsonProperty("aimStars")
    private Double aimStars;

    @JsonProperty("maxCombo")
    private long maxCombo;
}
