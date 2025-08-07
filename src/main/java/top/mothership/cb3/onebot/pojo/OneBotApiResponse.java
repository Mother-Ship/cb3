package top.mothership.cb3.onebot.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class OneBotApiResponse<T>{
    private String status;
    @SerializedName("retcode")
    private int code;
    private T data;
    private String echo;
}