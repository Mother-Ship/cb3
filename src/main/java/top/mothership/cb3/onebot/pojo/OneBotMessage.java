package top.mothership.cb3.onebot.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OneBotMessage {
    public interface OneBotApiParams {
    }

    // 发送群消息参数
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendGroupMsgParams implements OneBotApiParams {
        private Long groupId;
        private String message;
        private Boolean autoEscape; // 可选，是否作为纯文本发送
    }

    // 发送私聊消息参数
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendPrivateMsgParams implements OneBotApiParams {
        private Long userId;
        private String message;
        private Boolean autoEscape; // 可选，是否作为纯文本发送
    }

    // 群组禁言参数
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetGroupBanParams implements OneBotApiParams {
        private Long groupId;
        private Long userId;
        private Integer duration; // 禁言时长，单位秒，0表示解除禁言
    }

    // 处理加群请求参数
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetGroupAddRequestParams implements OneBotApiParams {
        private String flag; // 请求flag，从群请求事件中获取
        private String subType; // add 或 invite
        private Boolean approve; // 是否同意请求
        private String reason; // 可选，拒绝理由
    }

    // 群组踢人参数
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetGroupKickParams implements OneBotApiParams {
        private Long groupId;
        private Long userId;
        private Boolean rejectAddRequest; // 可选，拒绝此人的加群请求
    }
    // 获取群成员信息参数
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetGroupMemberListParams implements OneBotApiParams {
        private Long groupId;
    }


}
