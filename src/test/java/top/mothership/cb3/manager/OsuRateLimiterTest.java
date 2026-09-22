package top.mothership.cb3.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.mothership.cb3.config.AppProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验限流器的三种关键行为：
 * 均匀放行（不突发）、429 后按 Retry-After 降速、交互式请求超时快速失败。
 */
class OsuRateLimiterTest {

    private AppProperties props(int v1, int v2, int token) {
        AppProperties properties = new AppProperties();
        properties.setOsuApiV1RatePerMinute(v1);
        properties.setOsuApiV2RatePerMinute(v2);
        properties.setOsuApiTokenRatePerMinute(token);
        return properties;
    }

    private long elapsedMsSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    @Test
    @DisplayName("600/min 时请求应按 ~100ms 间隔逐个放行，而不是一次性冲出去")
    void acquireShouldBeSpacedOut() {
        OsuRateLimiter limiter = new OsuRateLimiter(props(600, 1200, 60));

        long start = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            limiter.acquire(OsuRateLimiter.BUCKET_V1);
        }
        long elapsedMs = elapsedMsSince(start);

        // 第 1 个立即返回，后 4 个各等 100ms，合计应接近 400ms
        assertTrue(elapsedMs >= 350, "5 次获取许可耗时 " + elapsedMs + "ms，说明没有按速率放行");
    }

    @Test
    @DisplayName("收到 429 后按 Retry-After 暂停，并把发送间隔翻倍（降级）")
    void rateLimitedShouldPauseAndDegrade() {
        OsuRateLimiter limiter = new OsuRateLimiter(props(600, 1200, 60)); // 间隔 100ms

        limiter.onRateLimited(OsuRateLimiter.BUCKET_V1, Duration.ofMillis(50)); // 暂停 50ms 且间隔 -> 200ms

        long start = System.nanoTime();
        for (int i = 0; i < 4; i++) {
            limiter.acquire(OsuRateLimiter.BUCKET_V1);
        }
        long elapsedMs = elapsedMsSince(start);

        // 第 1 次要等 50ms 暂停，之后 3 个间隔各 200ms，合计约 650ms
        assertTrue(elapsedMs >= 550, "降级后 4 次获取只用了 " + elapsedMs + "ms，说明降速没生效");
    }

    @Test
    @DisplayName("交互式请求遇到长 Retry-After 应立即失败，而不是干等")
    void interactiveAcquireShouldFailFastWhilePaused() {
        OsuRateLimiter limiter = new OsuRateLimiter(props(600, 1200, 60));
        limiter.onRateLimited(OsuRateLimiter.BUCKET_V1, Duration.ofSeconds(600));

        long start = System.nanoTime();
        assertThrows(OsuApiUnavailableException.class,
                () -> limiter.acquire(OsuRateLimiter.BUCKET_V1, Duration.ofSeconds(3)));
        long elapsedMs = elapsedMsSince(start);

        assertTrue(elapsedMs < 1000, "交互式请求等了 " + elapsedMs + "ms 才失败，没有快速降级");
    }

    @Test
    @DisplayName("批量请求（不设上限）会一直等到暂停窗口结束")
    void batchAcquireShouldWaitOutThePause() {
        OsuRateLimiter limiter = new OsuRateLimiter(props(600, 1200, 60));
        limiter.pauseFor(OsuRateLimiter.BUCKET_V1, Duration.ofMillis(300));

        long start = System.nanoTime();
        limiter.acquire(OsuRateLimiter.BUCKET_V1);
        long elapsedMs = elapsedMsSince(start);

        assertTrue(elapsedMs >= 250, "暂停期间批量请求只等待了 " + elapsedMs + "ms");
    }

    @Test
    @DisplayName("不同桶之间互不影响，v2 不会被 v1 的暂停拖住")
    void bucketsAreIndependent() {
        OsuRateLimiter limiter = new OsuRateLimiter(props(60, 1200, 60));
        limiter.pauseFor(OsuRateLimiter.BUCKET_V1, Duration.ofSeconds(5));

        long start = System.nanoTime();
        limiter.acquire(OsuRateLimiter.BUCKET_V2);
        long elapsedMs = elapsedMsSince(start);

        assertTrue(elapsedMs < 250, "v2 桶被 v1 的暂停影响了，耗时 " + elapsedMs + "ms");
    }
}
