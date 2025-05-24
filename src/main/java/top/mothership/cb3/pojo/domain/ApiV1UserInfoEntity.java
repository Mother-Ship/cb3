package top.mothership.cb3.pojo.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("userinfo")
/**
 * 这里用基础类型，是因为ppy的API V1返回 在数据为0的时候返回null，借助基础类型进行初始化
 */
public class ApiV1UserInfoEntity {
    private Integer id;

    private Integer mode;

    private int userId;

    private int count300;

    private int count100;

    private int count50;

    @TableField("playcount")
    private int playCount;

    private float accuracy;

    private float ppRaw;

    private long rankedScore;

    private long totalScore;

    private float level;

    private int ppRank;

    private int countRankSs;

    private int countRankSsh;

    private int countRankS;

    private int countRankSh;

    private int countRankA;

    private LocalDate queryDate;

}
