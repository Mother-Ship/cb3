package top.mothership.cb3.pojo.osu.apiv1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * get_scores 接口返回数据封装类
 * 用于封装指定谱面的前100名（存疑）分数信息
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiV1BeatmapScoreVO  extends  BaseApiV1ScoreVO{
    private String scoreId;

    private String username;

    private String pp;

    private String replayAvailable;
}
