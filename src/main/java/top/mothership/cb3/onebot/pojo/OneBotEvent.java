package top.mothership.cb3.onebot.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class OneBotEvent {



    // 基础事件类
    @Data
    public abstract static class BaseEvent {
        private Long time;
        @JsonProperty("self_id")
        private Long selfId;
        @JsonProperty("post_type")
        private String postType;
    }


// ==================== 消息事件 ====================


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MessageEvent extends BaseEvent {
        @JsonProperty("message_type")
        private String messageType; // private, group

        @JsonProperty("sub_type")
        private String subType; // friend, group, normal, anonymous, notice, other

        @JsonProperty("message_id")
        private Integer messageId;

        @JsonProperty("user_id")
        private Long userId;

        private String message; // 消息内容

        @JsonProperty("raw_message")
        private String rawMessage; // 原始消息，Onebot设置为CQ码上报时和message一样

        private Integer font;

        private SenderInfo sender;

        @JsonProperty("group_id")
        private Long groupId;

        private Anonymous anonymous; // 匿名信息

        // 匿名信息
        @Data
        public static class Anonymous {
            private Long id;
            private String name;
            private String flag;
        }

        // 发送者信息
        @Data
        public static class SenderInfo {
            @JsonProperty("user_id")
            private Long userId;
            private String nickname;
            private String sex;
            private Integer age;
            private String card; // 群名片
            private String area;
            private String level;
            private String role; // 群角色：owner/admin/member
            private String title; // 群头衔
        }
    }


// ==================== 加群邀请事件（请求事件）====================

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GroupRequestEvent extends BaseEvent {
        @JsonProperty("request_type")
        private String requestType; // group
        @JsonProperty("sub_type")
        private String subType; // add, invite
        @JsonProperty("group_id")
        private Long groupId;
        @JsonProperty("user_id")
        private Long userId;
        private String comment; // 验证信息
        private String flag; // 请求flag，用于处理请求
    }
// ==================== 新增群成员事件（通知事件）====================

// ==================== 群成员减少事件 ====================

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GroupIncreaseNoticeEvent extends BaseEvent {
        @JsonProperty("notice_type")
        private String noticeType; // group_increase
        @JsonProperty("sub_type")
        private String subType; // approve, invite
        @JsonProperty("group_id")
        private Long groupId;
        @JsonProperty("operator_id")
        private Long operatorId; // 操作者QQ号
        @JsonProperty("user_id")
        private Long userId; // 加入者QQ号
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GroupDecreaseNoticeEvent extends BaseEvent {
        @JsonProperty("notice_type")
        private String noticeType; // group_decrease
        @JsonProperty("sub_type")
        private String subType; // leave, kick, kick_me
        @JsonProperty("group_id")
        private Long groupId;
        @JsonProperty("operator_id")
        private Long operatorId; // 操作者QQ号
        @JsonProperty("user_id")
        private Long userId; // 离开者QQ号
    }

// ==================== 心跳事件（元事件）====================

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HeartbeatMetaEvent extends BaseEvent {
        @JsonProperty("meta_event_type")
        private String metaEventType; // heartbeat
        private Status status; // 状态信息
        private Long interval; // 心跳间隔(毫秒)
        // 状态信息
        @Data
        public static class Status {
            private Boolean online; // 是否在线
            private Boolean good; // 状态是否正常
            @JsonProperty("stat")
            private StatInfo stat; // 统计信息
        }

        // 统计信息
        @Data
        public static class StatInfo {
            @JsonProperty("packet_received")
            private Long packetReceived; // 收包数
            @JsonProperty("packet_sent")
            private Long packetSent; // 发包数
            @JsonProperty("packet_lost")
            private Long packetLost; // 丢包数
            @JsonProperty("message_received")
            private Long messageReceived; // 接收消息数
            @JsonProperty("message_sent")
            private Long messageSent; // 发送消息数
            @JsonProperty("disconnect_times")
            private Long disconnectTimes; // 断线次数
            @JsonProperty("lost_times")
            private Long lostTimes; // 丢失次数
            @JsonProperty("last_message_time")
            private Long lastMessageTime; // 最后消息时间
        }

    }





}
