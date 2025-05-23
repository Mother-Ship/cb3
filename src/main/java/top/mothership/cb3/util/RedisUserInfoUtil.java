package top.mothership.cb3.util;


import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.mothership.cb3.pojo.domain.ApiV1UserInfoEntity;

import java.util.concurrent.TimeUnit;

@Component
@AllArgsConstructor
public class RedisUserInfoUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    public void addUserInfoToHash(Integer userId, ApiV1UserInfoEntity userinfo) {
        redisTemplate.opsForHash().put(String.valueOf(userId), String.valueOf(userinfo.getMode()), userinfo);
    }

    public ApiV1UserInfoEntity getUserInfoByMode(Integer userId, Integer mode) {
        return (ApiV1UserInfoEntity) redisTemplate.opsForHash().get(String.valueOf(userId), String.valueOf(mode));
    }

    public void expire(Integer userId, final long timeout, final TimeUnit unit) {
        redisTemplate.expire(String.valueOf(userId), timeout, unit);
    }

    public void flushDb() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
}
