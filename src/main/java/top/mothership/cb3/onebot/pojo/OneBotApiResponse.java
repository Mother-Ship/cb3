package top.mothership.cb3.onebot.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OneBotApiResponse<T>{
    private String status;
    @JsonProperty("retcode")
    private int code;
    private T data;
    private String echo;
}