package top.mothership.cb3.pojo.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("userinfo")
public class ApiV1UserInfoEntity {
    private Integer id;

    private Integer mode;

    private int userId;

    private Integer count300;

    private Integer count100;

    private Integer count50;

    @TableField("playcount")
    private Integer playCount;

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
