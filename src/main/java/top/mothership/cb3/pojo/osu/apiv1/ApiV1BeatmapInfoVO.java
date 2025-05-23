package top.mothership.cb3.pojo.osu.apiv1;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * get_beatmaps 接口返回数据封装类
 * 用于封装谱面基本信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiV1BeatmapInfoVO {

    private String approved;

    private String submitDate;

    private String approvedDate;

    private String lastUpdate;

    private String artist;

    private String beatmapId;

    private String beatmapsetId;

    private String bpm;

    private String creator;

    private String creatorId;

    @JsonProperty("difficultyrating")
    private String difficultyRating;

    private String diffAim;

    private String diffSpeed;

    private String diffSize;

    private String diffOverall;

    private String diffApproach;

    private String diffDrain;

    private String hitLength;

    private String source;

    private String genreId;

    private String languageId;

    private String title;

    private String totalLength;

    private String version;

    private String fileMd5;

    private String mode;

    private String tags;

    private String favouriteCount;

    private String rating;

    @JsonProperty("playcount")
    private String playCount;

    @JsonProperty("passcount")
    private String passCount;

    private String countNormal;

    private String countSlider;

    private String countSpinner;

    private String maxCombo;

    private String storyboard;

    private String video;

    private String downloadUnavailable;

    private String audioUnavailable;
}