package top.mothership.cb3.pojo.old;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDate;

/**
 * 为兼容以前代码 字段名没有自动转下划线，因此写一个序列化为JSON后字段名也是驼峰的类
 */
@Data
public class OldCabbageUserInfoVO {
    private Integer id;

    private Integer mode;

    @JsonProperty("userId")
    private int userId;

    private int count300;

    private int count100;

    private int count50;

    @JsonProperty("playcount")
    private int playCount;

    private float accuracy;

    @JsonProperty("ppRaw")
    private float ppRaw;

    @JsonProperty("rankedScore")
    private long rankedScore;

    @JsonProperty("totalScore")
    private long totalScore;

    private float level;

    @JsonProperty("ppRank")
    private int ppRank;

    @JsonProperty("countRankSs")
    private int countRankSs;

    @JsonProperty("countRankSsh")
    private int countRankSsh;

    @JsonProperty("countRankS")
    private int countRankS;

    @JsonProperty("countRankSh")
    private int countRankSh;

    @JsonProperty("countRankA")
    private int countRankA;

    @JsonProperty("queryDate")
    @JsonSerialize(using = LocalDateCustomSerializer.class)
    private LocalDate queryDate;

}
