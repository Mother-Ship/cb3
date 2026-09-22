package top.mothership.cb3.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    /**
     * 默认的 RestTemplate 没有任何超时设置，osu! API 卡住时会一直占用线程，
     * 在录入这种高频场景下会迅速把线程池拖死；这里显式设置连接/读取超时。
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
