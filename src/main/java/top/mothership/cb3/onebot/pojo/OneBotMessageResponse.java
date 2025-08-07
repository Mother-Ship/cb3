package top.mothership.cb3.onebot.pojo;

import lombok.Data;

public class OneBotMessageResponse {
    // API响应数据类

    // 发送消息响应数据
    @Data
    public static class SendMsgResponse {
        private Integer messageId;
    }
}
