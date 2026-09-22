package top.mothership.cb3.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片下载配置属性类
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String cachePath;
    private int maxCacheCoverSize;

    /**
     * rosu-pp-js-http PP计算服务地址
     */
    private String ppCalcUrl;

    /**
     * osu! API v1 旧接口限流（次/分钟）。
     * 实测 /api/** 不返回限流响应头，超限为 Cloudflare 1015（HTTP 429）。
     * 默认 600，比官方文档的 1000/min 保守，实测该速率可持续发送而不触发 429。
     */
    private int osuApiV1RatePerMinute = 600;

    /**
     * osu! API v2 数据接口限流（次/分钟）。实测响应头 x-ratelimit-limit = 1200，这里留出余量。
     */
    private int osuApiV2RatePerMinute = 1100;

    /**
     * osu! API v2 令牌接口限流（次/分钟）。实测响应头 x-ratelimit-limit = 60。
     */
    private int osuApiTokenRatePerMinute = 60;
}