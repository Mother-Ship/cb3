package top.mothership.cb3.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cb3.manager.ApiManager;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.mapper.UserInfoDAO;
import top.mothership.cb3.pojo.domain.ApiV1UserInfoEntity;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.pojo.osu.apiv1.ApiV1UserInfoVO;
import top.mothership.cb3.util.RedisUserInfoUtil;
import top.mothership.cb3.util.UserRoleDataUtil;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ImportTask {
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


    @Scheduled(cron = "0 8 4 * * ?")
    @SneakyThrows
    public void importUserInfo() {

        redisUserInfoUtil.flushDb();
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));

        Set<String> bannedList = new LinkedHashSet<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger bindedCount = new AtomicInteger(0);

        // 先查出所有被查询过的玩家
        List<Integer> list = userDAO.listUserIdByRole(null, false);
        for (Integer userId : list) {

            UserRoleEntity user = userDAO.getUser(null, userId);

            //如果上次活跃在3个月之前，或者没绑定，不录入
            boolean skip = LocalDate.now().minusDays(90).isAfter(user.getLastActiveDate())
                    || user.getQq() == 0;

            // 如果玩家排名小于10000，则不跳过
            ApiV1UserInfoEntity nearestUserInfo = userInfoDAO.getNearestUserInfo(0, userId, LocalDate.now().minusDays(2));
            if (nearestUserInfo == null || nearestUserInfo.getPpRank() < 10000) {
                skip = false;
            }

            if (skip) {
                continue;
            }

            // 开始录入
            for (int mode = 0; mode < 4; mode++) {

                getUserRoleEntity(userId, mode, user, bannedList, successCount, bindedCount);

            }
        }
    }

    @NotNull
    private void getUserRoleEntity(Integer userId, int mode, UserRoleEntity user, Set<String> bannedList, AtomicInteger successCount, AtomicInteger bindedCount) throws JsonProcessingException {
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
        userInfoDAO.addUserInfo(entity);


        //同时加入redis缓存
        redisUserInfoUtil.addUserInfoToHash(userId, entity);
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
                bindedCount.addAndGet(1);
            }
        }


        user.setBanned(false);
        userDAO.updateUser(user);

    }

}
