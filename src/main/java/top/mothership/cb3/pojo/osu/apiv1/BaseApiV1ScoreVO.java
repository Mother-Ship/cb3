package top.mothership.cb3.pojo.osu.apiv1;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基础分数类，包含所有分数相关的公共字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseApiV1ScoreVO {

    private String score;

    @JsonProperty("maxcombo")
    private String maxCombo;

    private String count50;

    private String count100;

    private String count300;

    @JsonProperty("countmiss")
    private String countMiss;

    @JsonProperty("countkatu")
    private String countKatu;

    @JsonProperty("countgeki")
    private String countGeki;

    private String perfect;

    private String enabledMods;

    @JsonProperty("user_id")
    private String userId;

    private String date;

    private String rank;
}