package top.mothership.cb3.command.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.manager.OsuApiV1Manager;
import top.mothership.cb3.manager.OsuApiV2Manager;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.util.ApiV2ModeHolder;

import java.time.LocalDate;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextDataAspect {

    private final UserDAO userDAO;
    private final OsuApiV1Manager osuApiV1Manager;
    private final OsuApiV2Manager osuApiV2Manager;

    @Around("@annotation(needContextData)")
    public Object fillContextData(ProceedingJoinPoint joinPoint, NeedContextData needContextData) throws Throwable {
        // 在方法执行前填充上下文数据
        boolean success = fillContextData(needContextData.value());
        if (!success) {
            log.warn("填充上下文数据失败，打断命令执行");
            return null;
        }
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("方法执行异常", e);
            return null;
        } finally {
            // 方法执行后清理上下文数据
            clearContextData();
        }
    }

    private boolean fillContextData(ContextDataEnum[] contextDataEnums) {
        // 上下文数据填充
        for (ContextDataEnum contextDataEnum : contextDataEnums) {
            log.info("填充上下文数据: {}", contextDataEnum);
            switch (contextDataEnum) {
                case USER_ROLE:
                    UserRoleEntity user = userDAO.getUser(DataContext.getSender().getQQ(), null);
                    if (user == null) {
                        //TODO 老白菜逐步下掉未绑定提示后 再在这里处理
//                        var param = new OneBotMessage.SendGroupMsgParams();
//                        param.setGroupId(DataContext.getSender().getGroupId());
//                        param.setMessage("未绑定osu账号，请使用!setid <osu用户名> 绑定");
//                        OneBotWebsocketHandler.sendMessage(DataContext.getSender().getSelfId(), param);
                        log.warn("未绑定osu账号，填充上下文信息失败，打断命令执行");
                        return false;
                    }
                    user.setLastActiveDate(LocalDate.now());
                    userDAO.updateUser(user);
                    DataContext.setUserRole(user);
                    break;
                case BONDED_API_V1_USERINFO:
                    // 这里假定注解里一定出现在userrole后面
                    user = DataContext.getUserRole();
                    var userInfo = osuApiV1Manager.getUserInfo(user.getMode(), user.getUserId());
                    if (userInfo == null) {
                        //TODO 老白菜逐步下掉被ban提示后 再在这里处理

                        log.warn("调用osu! APIV1查询用户返回空，填充上下文信息失败，打断命令执行");
                        return false;
                    }
                    DataContext.setApiV1UserInfo(userInfo);
                    break;
                case BONDED_API_V2_USERINFO:
                    user = DataContext.getUserRole();
                    var userInfoV2 = osuApiV2Manager.getUserInfo(
                            ApiV2ModeHolder.fromInt(user.getMode()), String.valueOf(user.getUserId()));
                    DataContext.setApiV2User(userInfoV2);
                    break;
            }
        }
        return true;
    }

    private void clearContextData() {
        DataContext.clear();
    }
}
