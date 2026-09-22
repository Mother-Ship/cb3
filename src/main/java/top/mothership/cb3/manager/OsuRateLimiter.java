package top.mothership.cb3.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.config.AppProperties;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * osu! API 统一限流器。所有对 osu.ppy.sh 的请求都应先在这里取一个许可。
 *
 * <p><b>线上实测结论</b>（2026-09-22，生产出口 IP 125.119.151.5，使用线上真实 ak/sk 探测）：</p>
 * <ul>
 *   <li>API v2 数据接口 {@code /api/v2/**}：响应头 {@code x-ratelimit-limit: 1200}（次/分钟）。</li>
 *   <li>API v2 令牌接口 {@code /oauth/token}：响应头 {@code x-ratelimit-limit: 60}（次/分钟）。</li>
 *   <li>API v1 旧接口 {@code /api/**}：<b>不返回任何限流响应头</b>。超限时返回
 *       {@code HTTP 429}，响应体为 {@code error code: 1015}（Cloudflare 边缘限流），并带 {@code Retry-After}。
 *       实测：约 600 次/分钟平滑发送 160s 无一次 429；数千次/分钟的并发冲击会立刻全量 429，
 *       且 {@code Retry-After} 会从约 100s 逐步升级到约 1800s。</li>
 * </ul>
 *
 * <p><b>策略</b>：均匀放行（不突发）+ 按 Retry-After 降级。收到 429 时：</p>
 * <ol>
 *   <li>整个桶按 {@code Retry-After} 暂停，期间不发任何请求（等满，不提前试探，否则会延长封禁）；</li>
 *   <li>把该桶的发送速率减半（降级），下限 {@value #MIN_RATE_PER_MINUTE} 次/分钟，
 *       避免"被打一次还按原速硬冲"；</li>
 *   <li>Retry-After 窗口结束后按 {@value #RECOVER_STEP_SECONDS} 秒一步、每步回升 25%，逐步恢复到配置速率。</li>
 * </ol>
 *
 * <p>交互式调用（QQ 命令）使用带 {@code maxWait} 的 {@link #acquire(String, Duration)}：
 * 一旦所需等待超过上限就<b>立即失败</b>而不是干等半小时；批量任务（每日录入）用
 * {@link #acquire(String)} 老老实实排队等满 Retry-After。</p>
 */
@Component
@Slf4j
public class OsuRateLimiter {

    /** osu! API v1 旧接口（/api/**） */
    public static final String BUCKET_V1 = "v1";
    /** osu! API v2 数据接口（/api/v2/**） */
    public static final String BUCKET_V2 = "v2";
    /** osu! API v2 令牌接口（/oauth/token） */
    public static final String BUCKET_TOKEN = "token";

    /** 降级下限：无论被打多少次，速率不再低于这个值，避免任务被彻底停死 */
    private static final int MIN_RATE_PER_MINUTE = 60;
    /** 降级后每步恢复的间隔 */
    private static final long RECOVER_STEP_SECONDS = 60;
    /** 每步恢复的比例：间隔乘以该系数（越小恢复越快） */
    private static final double RECOVER_FACTOR = 0.75;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public OsuRateLimiter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * 阻塞获取一个许可，允许一直等到 Retry-After 结束。用于批量任务（每日录入）。
     */
    public void acquire(String bucketName) {
        bucket(bucketName).acquire(null);
    }

    /**
     * 获取一个许可，但最多只等 {@code maxWait}。超时（通常是因为桶正处于 429 的暂停窗口）
     * 时抛 {@link OsuApiUnavailableException} 立即失败，用于交互式命令。
     *
     * @param maxWait 最大等待时长；传 {@code null} 表示不设上限
     */
    public void acquire(String bucketName, Duration maxWait) {
        bucket(bucketName).acquire(maxWait);
    }

    /**
     * 收到 429 时调用：按 {@code Retry-After} 暂停该桶并降速。
     *
     * @param retryAfter 响应头 Retry-After；为 null 时只降速不暂停
     */
    public void onRateLimited(String bucketName, Duration retryAfter) {
        Bucket bucket = bucket(bucketName);
        if (retryAfter != null && !retryAfter.isNegative() && !retryAfter.isZero()) {
            bucket.pause(retryAfter.toNanos());
        }
        bucket.degrade();
        log.warn("osu! API 限流桶[{}] 收到 429：暂停 {} 秒并降速，当前发送间隔 {} ms（配置速率 {} 次/分钟）",
                bucketName,
                retryAfter == null ? 0 : retryAfter.toSeconds(),
                bucket.currentIntervalMillis(),
                ratePerMinute(bucketName));
    }

    /**
     * 只暂停不降速。保留给确实只是想短暂让路的场景。
     */
    public void pauseFor(String bucketName, Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        log.warn("osu! API 限流桶[{}]暂停放行 {} 秒", bucketName, duration.toSeconds());
        bucket(bucketName).pause(duration.toNanos());
    }

    private Bucket bucket(String bucketName) {
        return buckets.computeIfAbsent(bucketName, name -> new Bucket(ratePerMinute(name)));
    }

    private int ratePerMinute(String bucketName) {
        return switch (bucketName) {
            case BUCKET_V1 -> appProperties.getOsuApiV1RatePerMinute();
            case BUCKET_V2 -> appProperties.getOsuApiV2RatePerMinute();
            case BUCKET_TOKEN -> appProperties.getOsuApiTokenRatePerMinute();
            default -> throw new IllegalArgumentException("未知的 osu! API 限流桶: " + bucketName);
        };
    }

    /**
     * 均匀放行的令牌桶：请求之间至少间隔 {@code currentIntervalNanos}，不允许突发。
     * 间隔会因 429 降级变大，并在恢复窗口内逐步回到配置值。
     */
    private static final class Bucket {
        private final long baseIntervalNanos;
        private final long maxIntervalNanos;
        private final Object lock = new Object();
        private long currentIntervalNanos;
        private long nextSlotNanos;
        private long pausedUntilNanos;
        private long recoverAtNanos;

        private Bucket(int permitsPerMinute) {
            this.baseIntervalNanos = Duration.ofMinutes(1).toNanos() / Math.max(1, permitsPerMinute);
            this.maxIntervalNanos = Duration.ofMinutes(1).toNanos() / MIN_RATE_PER_MINUTE;
            this.currentIntervalNanos = baseIntervalNanos;
        }

        private void acquire(Duration maxWait) {
            long maxWaitNanos = maxWait == null ? Long.MAX_VALUE : Math.max(0, maxWait.toNanos());
            while (true) {
                long waitNanos;
                synchronized (lock) {
                    long now = System.nanoTime();
                    maybeRecover(now);
                    long floor = Math.max(now, pausedUntilNanos);
                    long slot = Math.max(nextSlotNanos, floor);
                    waitNanos = slot - now;
                    if (waitNanos > maxWaitNanos) {
                        long needNanos = Math.max(waitNanos, pausedUntilNanos - now);
                        throw new OsuApiUnavailableException("osu! API 限流中，按 Retry-After 还需等待约 "
                                + (TimeUnit.NANOSECONDS.toSeconds(needNanos) + 1) + " 秒，超过本次允许的 "
                                + maxWait.toSeconds() + " 秒，已降级为立即失败");
                    }
                    nextSlotNanos = slot + currentIntervalNanos;
                }
                if (waitNanos <= 0) {
                    return;
                }
                LockSupport.parkNanos(waitNanos);
                synchronized (lock) {
                    // 等待期间可能又收到了 429 触发的暂停，此时重新排队
                    if (System.nanoTime() >= pausedUntilNanos) {
                        return;
                    }
                }
            }
        }

        /** 429 暂停：暂停窗口同时作为降速的恢复起点 */
        private void pause(long nanos) {
            synchronized (lock) {
                long until = System.nanoTime() + nanos;
                if (until > pausedUntilNanos) {
                    pausedUntilNanos = until;
                }
                if (until > recoverAtNanos) {
                    recoverAtNanos = until;
                }
            }
        }

        /** 降速：发送间隔翻倍，最多到 MIN_RATE_PER_MINUTE 对应的间隔 */
        private void degrade() {
            synchronized (lock) {
                if (currentIntervalNanos < maxIntervalNanos) {
                    currentIntervalNanos = Math.min(maxIntervalNanos, currentIntervalNanos * 2);
                }
                long step = TimeUnit.SECONDS.toNanos(RECOVER_STEP_SECONDS);
                recoverAtNanos = Math.max(recoverAtNanos, System.nanoTime() + step);
            }
        }

        /** 每 RECOVER_STEP_SECONDS 让间隔向配置值回退一步 */
        private void maybeRecover(long now) {
            if (now < recoverAtNanos || currentIntervalNanos <= baseIntervalNanos) {
                return;
            }
            currentIntervalNanos = Math.max(baseIntervalNanos, (long) (currentIntervalNanos * RECOVER_FACTOR));
            recoverAtNanos = now + TimeUnit.SECONDS.toNanos(RECOVER_STEP_SECONDS);
        }

        private long currentIntervalMillis() {
            synchronized (lock) {
                return TimeUnit.NANOSECONDS.toMillis(currentIntervalNanos);
            }
        }
    }
}
