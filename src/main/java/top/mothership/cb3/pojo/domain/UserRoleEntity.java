package top.mothership.cb3.pojo.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("userrole")
public class UserRoleEntity {
    private Integer userId;
    private String role;

    private Long qq;
    private String legacyUname;
    private String currentUname;
    private boolean banned;
    private Integer mode;
    private Long repeatCount;
    private Long speakingCount;
    private String mainRole;
    private LocalDate lastActiveDate;
}
