package top.mothership.cb3.onebot.pojo;

import lombok.Data;

@Data
public class OneBotApiRequest<T extends OneBotMessage.OneBotApiParams> {
    private String echo;
    private String action;
    private T params;
}
