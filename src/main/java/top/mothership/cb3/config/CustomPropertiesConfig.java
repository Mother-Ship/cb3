package top.mothership.cb3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cabbage")
@Data
public class CustomPropertiesConfig {
    private String accountForDL;
    private String accountForDLPwd;
    private String apikey;
    private String apiV2Secret;
    private String apiV2Id;
}