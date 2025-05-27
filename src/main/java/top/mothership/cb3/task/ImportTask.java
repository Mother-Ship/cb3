package top.mothership.cb3.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cb3.manager.ApiManager;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.mapper.UserInfoDAO;
import top.mothership.cb3.pojo.domain.ApiV1UserInfoEntity;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.pojo.old.OldCabbageUserInfoVO;
import top.mothership.cb3.pojo.osu.apiv1.ApiV1UserInfoVO;
import top.mothership.cb3.util.RedisUserInfoUtil;
import top.mothership.cb3.util.UserRoleDataUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ImportTask {
    // 线程池配置
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // 固定大小线程池
    private final Semaphore semaphore = new Semaphore(600); // 每分钟最多触发 600 次
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    @Autowired
    private RedisUserInfoUtil redisUserInfoUtil;
    @Autowired
    private UserInfoDAO userInfoDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private ApiManager apiManager;
    @Autowired
    private UserRoleDataUtil userRoleDataUtil;

    public ImportTask() {
        // 定时任务：每分钟释放 1000 个许可
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(1000 - semaphore.availablePermits());
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Scheduled(cron = "0 8 4 * * ?")
    @SneakyThrows
    public void importUserInfo() {

        log.info("开始导入玩家信息");
        redisUserInfoUtil.flushDb();
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));

        Set<String> bannedList = new LinkedHashSet<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger boundCount = new AtomicInteger(0);

        // 先查出所有被查询过的玩家
        List<Integer> list = userDAO.listUserIdByRole(null, false);

        // 计算所有不跳过的
        Map<Integer, UserRoleEntity> userMap = new HashMap<>();

        for (Integer userId : list) {

            UserRoleEntity user = userDAO.getUser(null, userId);

            // 绑了QQ并且1年内活跃的录入
            boolean skip = LocalDate.now().minusDays(365).isAfter(user.getLastActiveDate())
                    || user.getQq() == 0;

            // 又或者如果玩家STD模式排名小于10000，则录入
            ApiV1UserInfoEntity nearestUserInfo = userInfoDAO.getNearestUserInfo(0, userId, LocalDate.now().minusDays(2));
            if (nearestUserInfo == null || nearestUserInfo.getPpRank() < 10000) {
                skip = false;
            }

            if (skip) {
                log.info("玩家{}跳过，因为1年内未活跃或者STD模式排名小于10000", userId);
                continue;
            }else {
                log.info("准备导入玩家{}", userId);
            }
            userMap.put(userId, user);

        }
        log.info("开始导入玩家信息，数据库内共{}玩家，预期录入共{}个玩家", list.size(), userMap.size());


        // 使用CountDownLatch等待所有线程完成
        CountDownLatch latch = new CountDownLatch(userMap.size() * 4);

        for (Integer userId : userMap.keySet()) {
            // 开始录入
            for (int mode = 0; mode < 4; mode++) {
                // 提交任务到线程池
                int finalMode = mode;
                threadPool.submit(() -> {
                    try {
                        semaphore.acquire(); // 获取信号量许可
                        doImportAnUserAndMode(userId, finalMode, userMap.get(userId), bannedList, successCount, boundCount);
                    } catch (Exception e) {
                        log.error("任务执行失败: {}", e.getMessage(), e);
                    } finally {
                        semaphore.release(); // 释放信号量许可
                        latch.countDown(); // 任务完成后计数器减一
                    }
                });
            }
        }

        // 等待所有线程完成
        latch.await();

        // 打印结果到日志
        log.info("录入完成，本次录入标明被封禁玩家： {}", bannedList);
        log.info("录入成功玩家： {}", successCount.get());
        log.info("其中已绑定QQ的玩家： {}", boundCount.get());
    }

    private void doImportAnUserAndMode(Integer userId, int mode, UserRoleEntity user, Set<String> bannedList, AtomicInteger successCount, AtomicInteger boundCount) throws JsonProcessingException {
        // 原有逻辑保持不变
        log.info("开始导入玩家{}，模式{}", userId, mode);
        ApiV1UserInfoVO userinfo = apiManager.getUserInfo(mode, userId);

        if (userinfo == null) {
            //将本次获取失败的用户直接设为banned
            if (!user.isBanned()) {
                user.setBanned(true);
                log.info("检测到玩家{}被Ban，已登记", user.getUserId());
                userDAO.updateUser(user);
            }

            bannedList.add(user.getCurrentUname());
            return;
        }

        //将日期改为一天前，插入数据库
        var entity = new ApiV1UserInfoEntity();
        BeanUtils.copyProperties(userinfo, entity);
        entity.setQueryDate(LocalDate.now().minusDays(1));
        entity.setMode(mode);
        userInfoDAO.addUserInfo(entity);


        //同时加入redis缓存
        var old = new OldCabbageUserInfoVO();
        BeanUtils.copyProperties(entity, old);
        redisUserInfoUtil.addUserInfoToHash(userId, old);
        redisUserInfoUtil.expire(userId, 1, TimeUnit.DAYS);


        if (!userinfo.getUsername().equals(user.getCurrentUname())) {
            //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
            log.info("检测到玩家{}改名，曾用名{}已登记", userinfo.getUsername(), user.getCurrentUname());
            user = userRoleDataUtil.renameUser(user, userinfo.getUsername());
            userDAO.updateUser(user);
        }

        if (mode == 0) {
            successCount.addAndGet(1);
            if (!user.getQq().equals(0L)) {
                boundCount.addAndGet(1);
            }
        }


        user.setBanned(false);
        userDAO.updateUser(user);

    }

}
