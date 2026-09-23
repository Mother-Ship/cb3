package top.mothership.cb3.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    /**
     * 显式指定 JDK 自带的 {@link SimpleClientHttpRequestFactory}。
     *
     * <p>classpath 上有 docker-java 带进来的 httpclient5，Spring Boot 会自动选中
     * {@code HttpComponentsClientHttpRequestFactory}；而 HC5 5.4+ 重写的解压实体
     * （DecompressingEntity/LazyDecompressingInputStream，见 HTTPCLIENT-2422）在 Spring
     * 读取响应体前的“读 1 字节判空再回推”逻辑下会直接返回 EOF，导致 gzip 响应只剩第一个字节——
     * 表现为 osu! 令牌接口返回 200 却反序列化失败。实测 5.5.1、5.6.3 均未修复，
     * 故换回 JDK 实现（自带 gzip 透明解压与 keep-alive）。</p>
     *
     * <p>默认的 RestTemplate 没有任何超时设置，osu! API 卡住时会一直占用线程，
     * 在录入这种高频场景下会迅速把线程池拖死；这里显式设置连接/读取超时。
     * 注意：自定义 requestFactory 时 builder 上的超时不会生效，必须直接设置在 factory 上。</p>
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofSeconds(10));
                    factory.setReadTimeout(Duration.ofSeconds(30));
                    return factory;
                })
                .build();
    }
}
