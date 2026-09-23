package top.mothership.cb3.pojo.osu.ppcalc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * PP计算服务的成绩计算结果
 */
@Data
public class PpScoreResult {

    @JsonProperty("aim")
    private double aim;
    @JsonProperty("speed")
    private double speed;
    @JsonProperty("acc")
    private double acc;
    @JsonProperty("pp")
    private double pp;
}
