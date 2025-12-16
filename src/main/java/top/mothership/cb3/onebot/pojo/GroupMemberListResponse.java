package top.mothership.cb3.onebot.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GroupMemberListResponse extends OneBotApiResponse<List<GroupMemberListResponse.GroupMemberInfo>> {

    @Data
    public static class GroupMemberInfo {
        /**
         * 年龄
         */
        private Double age;

        /**
         * 地区
         */
        private String area;

        /**
         * 群昵称
         */
        private String card;

        /**
         * 群昵称是否可修改
         */
        private Boolean cardChangeable;

        private Long groupId;

        /**
         * 是否机器人
         */
        private Boolean isRobot;

        /**
         * 加群时间
         */
        private Long joinTime;

        /**
         * 最后发言时间
         */
        private Long lastSentTime;

        /**
         * 群等级
         */
        private String level;

        private String nickname;

        /**
         * Q龄
         */
        private String qage;

        /**
         * 账号等级
         */
        private Long qqLevel;

        /**
         * 权限
         */
        private String role;

        /**
         * 性别
         */
        private String sex;

        /**
         * 禁言时间戳
         */
        private Long shutUpTimestamp;

        /**
         * 头衔
         */
        private String title;

        /**
         * 头衔过期时间
         */
        private Long titleExpireTime;

        private Boolean unfriendly;

        private Long userId;
    }
}
