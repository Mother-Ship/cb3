package top.mothership.cb3.pojo.osu.apiv1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * get_user_best 接口返回数据封装类
 * 用于封装用户BP信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiV1UserBestScoreVO extends BaseApiV1ScoreVO {
    private String beatmapId;

    private String scoreId;

    private String pp;

    private String replayAvailable;

    private byte mode;
}
