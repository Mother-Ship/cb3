package top.mothership.cb3.command.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.aop.NeedContextData;
import top.mothership.cb3.command.argument.RecentCommandArg;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.reflect.CbCmdProcessor;
import top.mothership.cb3.manager.OsuApiV2Manager;
import top.mothership.cb3.manager.constant.ApiV2ModeHolder;
import top.mothership.cb3.pojo.osu.apiv2.request.UserScoresRequest;

@Component
@Slf4j
public class RecentCommandHandler {
    private final OsuApiV2Manager osuApiV2Manager;

    public RecentCommandHandler(OsuApiV2Manager osuApiV2Manager) {
        this.osuApiV2Manager = osuApiV2Manager;
    }

    @CbCmdProcessor({"pr", "recent"})
    @NeedContextData({ContextDataEnum.USER_ROLE, ContextDataEnum.BONDED_API_V1_USERINFO})
    public void pr(RecentCommandArg command) {
        var sender = DataContext.getSender();
        var userRole = DataContext.getUserRole();
        log.info("正在处理{}发送的命令 pr，参数mode：{}，绑定用户{}", sender.getQQ(), command.getMode(), userRole.getCurrentUname());
        var mode = command.getMode() == null ? userRole.getMode() : Integer.valueOf(command.getMode());
        var apiV2Mode = ApiV2ModeHolder.fromInt(mode);
        if (userRole.isUseLazer()) {
            var recent = osuApiV2Manager.getUserRecentScores(
                    new UserScoresRequest(
                            String.valueOf(userRole.getUserId()),
                            "recent",
                            1, 0,
                            true,
                            false, apiV2Mode));
            log.info("获取到{}的{}最近成绩：{}", userRole.getCurrentUname(), apiV2Mode, recent);
        }
    }
}
