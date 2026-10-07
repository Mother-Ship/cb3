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
     *
     * <p>/api/** 不返回限流响应头，超限时 Cloudflare 直接返回 1015（HTTP 429）。2026-10-08 用真实
     * apikey 从另一个出口 IP（103.151.172.93，与生产机无关）实测发现：真正的约束<b>不是</b>
     * "每分钟次数"，而是<b>整个 key 在一个窗口内（约 5 分钟）的总请求数 ≈ 1800 次</b>——
     * 300 次/分钟只发到 966 次（193 秒）就被 1015 拦下，生产机 600 次/分钟也只撑了 207 秒
     * （约 1800 次）就再次被封。所以"降速"并不能增加总额度，只能把速率压到
     * 1800 / 300s ≈ 360 次/分钟以下并留出余量。</p>
     *
     * <p>默认取 240（约留 33% 余量）。另外配额是按 key 记账的，同 key 的其它消费者
     * （如老白菜 cabbageWeb）会一起分摊，所以不要贴着 360 配。</p>
     */
    private int osuApiV1RatePerMinute = 240;

    /**
     * osu! API v2 数据接口限流（次/分钟）。实测响应头 x-ratelimit-limit = 1200，这里留出余量。
     */
    private int osuApiV2RatePerMinute = 1100;

    /**
     * osu! API v2 令牌接口限流（次/分钟）。实测响应头 x-ratelimit-limit = 60。
     */
    private int osuApiTokenRatePerMinute = 60;
}