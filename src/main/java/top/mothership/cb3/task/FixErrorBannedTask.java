package top.mothership.cb3.task;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cb3.manager.OsuApiUnavailableException;
import top.mothership.cb3.manager.OsuApiV1Manager;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.mapper.UserInfoDAO;
import top.mothership.cb3.pojo.domain.ApiV1UserInfoEntity;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.pojo.osu.apiv1.ApiV1UserInfoVO;
import top.mothership.cb3.util.UserRoleDataUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时修复“误封禁”的玩家。
 *
 * <p>玩家被封禁后仍可能被查询，若这时 osu! API 恰好限流/故障，旧逻辑会把“查不到”误判成“被封禁”。
 * 本任务每小时把数据库里标记为封禁的玩家重新查一遍：只要接口能正常返回数据，就补录当日数据并解除封禁。</p>
 *
 * <p><b>迁移说明</b>：本任务原位于 cabbageWeb（老白菜）项目，与 cb3 共用同一个 osu! API key。
 * 两个进程各自发送 v1 请求、互不知晓对方的发送量，叠加后持续触发 Cloudflare 1015（HTTP 429）。
 * 迁移到 cb3 后，请求统一经过 {@link top.mothership.cb3.manager.OsuRateLimiter} 排队，两个定时任务共用
 * 同一份配额，不再互相抢份额。</p>
 */
@Component
@Slf4j
public class FixErrorBannedTask {

    /**
     * 只查 STD（mode 0）即可判断账号是否仍然有效，不必查四个模式，避免浪费共享配额。
     */
    private static final int MODE_STD = 0;

    /**
     * 定时触发线程和实际工作线程分开：单线程调度器被长时间占住会影响其它 @Scheduled 任务，
     * 而限流退避时本任务可能阻塞很久，因此丢到独立线程里执行。
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "fix-error-banned-task");
        thread.setDaemon(true);
        return thread;
    });

    /** 防止上一次还没跑完就被下一次 cron 触发出第二个实例。 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserInfoDAO userInfoDAO;

    @Autowired
    private OsuApiV1Manager osuApiV1Manager;

    @Autowired
    private UserRoleDataUtil userRoleDataUtil;

    @Scheduled(cron = "0 0 * * * ?")
    public void refreshBannedStatus() {
        if (!running.compareAndSet(false, true)) {
            log.warn("上一次误封修复尚未结束，本次跳过");
            return;
        }
        executor.submit(() -> {
            try {
                doRefreshBannedStatus();
            } catch (Exception e) {
                log.error("误封修复任务执行失败", e);
            } finally {
                running.set(false);
            }
        });
    }

    private void doRefreshBannedStatus() {
        List<UserRoleEntity> list = userDAO.listBannedUser();
        if (list.isEmpty()) {
            log.info("误封修复：当前没有被标记封禁的玩家");
            return;
        }

        int fixed = 0;
        int stillBanned = 0;
        int failed = 0;
        for (UserRoleEntity user : list) {
            ApiV1UserInfoVO userinfo;
            try {
                // 批量任务，允许老实排队等满 Retry-After；与每日录入共用统一限流器
                userinfo = osuApiV1Manager.getUserInfoForImport(MODE_STD, user.getUserId());
            } catch (OsuApiUnavailableException e) {
                // 限流/故障导致拿不到数据，绝不能当成“仍然被封禁”或“已解封”，保持原状等下一轮
                failed++;
                log.warn("误封修复：玩家{}查询失败（接口限流/故障，非封禁），本轮跳过：{}",
                        user.getUserId(), e.getMessage());
                continue;
            }

            if (userinfo == null) {
                // 接口正常但查不到该玩家，说明确实被封禁，保持封禁状态
                stillBanned++;
                continue;
            }

            // 将日期改为一天前写入，与每日录入的补录口径一致
            ApiV1UserInfoEntity entity = new ApiV1UserInfoEntity();
            BeanUtils.copyProperties(userinfo, entity);
            entity.setMode(MODE_STD);
            entity.setQueryDate(LocalDate.now().minusDays(1));
            userInfoDAO.addUserInfo(entity);

            if (!Objects.equals(userinfo.getUsername(), user.getCurrentUname())) {
                // 如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
                log.info("检测到玩家{}改名，曾用名{}已登记", userinfo.getUsername(), user.getCurrentUname());
                user = userRoleDataUtil.renameUser(user, userinfo.getUsername());
            }

            // 能拿到 userinfo，说明账号正常，解除误封
            user.setBanned(false);
            userDAO.updateUser(user);
            fixed++;
            log.info("将{}的数据补录成功，已解除误封", userinfo.getUsername());
        }

        log.info("误封修复完成：解封 {} 个，确认仍封禁 {} 个，因接口限流/故障跳过 {} 个",
                fixed, stillBanned, failed);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
