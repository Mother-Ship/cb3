package top.mothership.cb3.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.mothership.cb3.manager.OsuApiUnavailableException;
import top.mothership.cb3.manager.OsuApiV1Manager;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.mapper.UserInfoDAO;
import top.mothership.cb3.pojo.domain.ApiV1UserInfoEntity;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.pojo.old.OldCabbageUserInfoVO;
import top.mothership.cb3.pojo.osu.apiv1.ApiV1UserInfoVO;
import top.mothership.cb3.util.RedisUserInfoUtil;
import top.mothership.cb3.util.UserRoleDataUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ImportTask {
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * 并发度。真正的发送速率由 {@link OsuApiV1Manager} 内部的 {@code OsuRateLimiter}
     * 统一控制，这里只决定有多少个请求可以同时在“限流器排队 + 网络往返”，不要设大。
     *
     * <p>旧实现用 {@code Semaphore(600)}+每分钟补满 1000 的方式限流，等于允许每分钟 1000 次
     * 突发请求，实测会触发 Cloudflare 1015（HTTP 429）；现改为限流器里均匀放行。</p>
     */
    private static final int IMPORT_THREADS = 8;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(IMPORT_THREADS);

    @Autowired
    private RedisUserInfoUtil redisUserInfoUtil;
    @Autowired
    private UserInfoDAO userInfoDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private OsuApiV1Manager osuApiV1Manager;
    @Autowired
    private UserRoleDataUtil userRoleDataUtil;

    @SneakyThrows
    @Async
    public void importUserInfo() {

        log.info("开始导入玩家信息");
        redisUserInfoUtil.flushDb();
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));

        // 多线程写入，必须用线程安全的集合
        Set<String> bannedList = Collections.synchronizedSet(new LinkedHashSet<>());
        Set<String> failedList = Collections.synchronizedSet(new LinkedHashSet<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger boundCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 先查出所有被查询过的玩家
        List<Integer> list = userDAO.listUserIdByRole(null, false);

        // 计算所有不跳过的
        Map<Integer, UserRoleEntity> userMap = new HashMap<>();

        List<Integer> skipped = new ArrayList<>();
        for (Integer userId : list) {

            UserRoleEntity user = userDAO.getUser(null, userId);

            // 绑了QQ并且1年内活跃的录入
            boolean skip = user.getLastActiveDate().isBefore(LocalDate.now().minusDays(365))
                    || user.getQq() == 0;
            if (!skip) {
                userMap.put(userId, user);
                continue;
            }

            // 如果不满足上述条件，那就看玩家STD模式排名是否小于10000，是则录入
            ApiV1UserInfoEntity nearestUserInfo = userInfoDAO.getNearestUserInfo(0, userId, LocalDate.now().minusDays(2));
            skip = !(nearestUserInfo == null || nearestUserInfo.getPpRank() < 10000);

            if (!skip) {
                userMap.put(userId, user);
            }else {
                skipped.add(userId);
            }
        }

        log.info("跳过的玩家ID数量：{}", skipped.size());

        var preparedInfo = "开始导入玩家信息，数据库内共" + list.size() + "玩家，预期录入共" + userMap.size() + "个玩家";
        log.info(preparedInfo);
        notifyOldCb(preparedInfo);

        // 使用CountDownLatch等待所有线程完成
        // 注意：latch 必须在提交任务前建好；固定线程池使用无界队列，submit 不会因为队列满而拒绝
        CountDownLatch latch = new CountDownLatch(userMap.size() * 4);

        for (Integer userId : userMap.keySet()) {
            // 开始录入
            for (int mode = 0; mode < 4; mode++) {
                // 提交任务到线程池
                int finalMode = mode;
                threadPool.submit(() -> {
                    try {
                        doImportAnUserAndMode(userId, finalMode, userMap.get(userId), bannedList, failedList,
                                successCount, boundCount, failedCount);
                    } catch (Exception e) {
                        log.error("任务执行失败: {}", e.getMessage(), e);
                    } finally {
                        latch.countDown(); // 任务完成后计数器减一
                    }
                });
            }
        }

        // 等待所有线程完成
        latch.await();

        // 打印结果到日志
        var result = "录入完成，本次录入标明被封禁玩家：" + bannedList +
                "录入成功玩家： " + successCount.get() +
                "其中已绑定QQ的玩家：" + boundCount.get() +
                "因接口限流/故障跳过的请求数：" + failedCount.get() + failedList;
        log.info(result);

        // 通知老白菜服务
        notifyOldCb(result);
    }

    private void notifyOldCb(String result) {

        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("info", result);

        RequestBody body = formBuilder.build();

        Request request = new Request.Builder()
                .url("http://localhost:8080/api/v1/importInfo")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Request failed, code: {}, message: {}", response.code(), response.message());
            }
        } catch (IOException e) {
            log.error("Error making request", e);
        }
    }

    private void doImportAnUserAndMode(Integer userId, int mode, UserRoleEntity user, Set<String> bannedList,
                                       Set<String> failedList, AtomicInteger successCount,
                                       AtomicInteger boundCount, AtomicInteger failedCount) throws JsonProcessingException {
        log.debug("开始导入玩家{}，模式{}", userId, mode);

        ApiV1UserInfoVO userinfo;
        try {
            // 录入是批量任务，没有用户在等，用 ForImport 版本老实排队等满 Retry-After
            userinfo = osuApiV1Manager.getUserInfoForImport(mode, userId);
        } catch (OsuApiUnavailableException e) {
            // 接口限流/故障时拿不到数据，这和“玩家被封禁”完全是两回事。
            // 旧代码在这里把失败当成 banned，一次 429 就会误封一大批正常玩家。
            failedCount.incrementAndGet();
            failedList.add(user.getCurrentUname() + "(mode " + mode + ")");
            log.warn("玩家{}模式{}查询失败（接口限流/故障，非封禁），本次跳过：{}", userId, mode, e.getMessage());
            return;
        }

        if (userinfo == null) {
            // 接口正常返回、但没有该玩家的数据，才认定为被封禁
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
