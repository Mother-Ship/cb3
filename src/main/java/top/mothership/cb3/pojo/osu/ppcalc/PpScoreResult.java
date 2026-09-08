package top.mothership.cb3.pojo.osu.ppcalc;

import lombok.Data;

/**
 * PP计算服务的成绩计算结果
 */
@Data
public class PpScoreResult {

    private double aim;
    private double speed;
    private double acc;
    private double pp;
}
