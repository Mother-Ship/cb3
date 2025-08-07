package top.mothership.cb3.command.context;

import top.mothership.cb3.onebot.pojo.OneBotContextData;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.pojo.osu.apiv1.ApiV1UserInfoVO;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2User;

public class DataContext {
    private static final ThreadLocal<OneBotContextData> sender = new ThreadLocal<>();
    private static final ThreadLocal<UserRoleEntity> userRole = new ThreadLocal<>();
    private static final ThreadLocal<ApiV1UserInfoVO> apiV1UserInfo = new ThreadLocal<>();
    private static final ThreadLocal<ApiV2User.User> apiV2User = new ThreadLocal<>();

    public static void setSender(OneBotContextData senderInfo) {
        sender.set(senderInfo);
    }

    public static OneBotContextData getSender() {
        return sender.get();
    }

    public static void setUserRole(UserRoleEntity userRoleEntity) {
        userRole.set(userRoleEntity);
    }

    public static UserRoleEntity getUserRole() {
        return userRole.get();
    }

    public static void setApiV1UserInfo(ApiV1UserInfoVO userInfoVO) {
        apiV1UserInfo.set(userInfoVO);
    }

    public static ApiV1UserInfoVO getApiV1UserInfo() {
        return apiV1UserInfo.get();
    }

    public static void setApiV2User(ApiV2User.User user) {
        apiV2User.set(user);
    }

    public static ApiV2User.User getApiV2User() {
        return apiV2User.get();
    }

    public static void clear() {
        sender.remove();
        userRole.remove();
        apiV1UserInfo.remove();
        apiV2User.remove();
    }

}