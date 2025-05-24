package top.mothership.cb3.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:cabbage.properties")
@Data
public class CustomPropertiesConfig {
    @Value("${accountForDL}")
    private String accountForDL;

    @Value("${accountForDLPwd}")
    private String accountForDLPwd;

    @Value("${apikey}")
    private String apikey;

    @Value("${apiV2Secret}")
    private String apiV2Secret;

    @Value("${apiV2Id}")
    private String apiV2Id;
}