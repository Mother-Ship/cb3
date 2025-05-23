package top.mothership.cb3.pojo.osu.apiv1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * get_user_recent 接口返回数据封装类
 * 用于封装用户最近游戏记录信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiV1UserRecentScoreVO extends BaseApiV1ScoreVO{
    private String beatmapId;

    private String pp;

    private String replayAvailable;
}
