package top.mothership.cb3.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.aop.NeedContextData;
import top.mothership.cb3.command.argument.MyBPCommandArg;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.image.HtmlGenerateService;
import top.mothership.cb3.command.image.ScreenShotService;
import top.mothership.cb3.command.reflect.CbCmdProcessor;
import top.mothership.cb3.manager.OsuApiV2Manager;
import top.mothership.cb3.pojo.osu.apiv2.request.UserScoresRequest;
import top.mothership.cb3.util.ApiV2ModeHolder;

@Component
@RequiredArgsConstructor
@Slf4j
public class MyBPCommandHandler {
    private final OsuApiV2Manager osuApiV2Manager;
    private final HtmlGenerateService htmlGeneratorService;
    private final ScreenShotService screenshotService;

    @CbCmdProcessor({"mybp", "bpme"})
    @NeedContextData({ContextDataEnum.USER_ROLE})
    public void myBP(MyBPCommandArg arg) {
        var sender = DataContext.getSender();
        var userRole = DataContext.getUserRole();

        log.info("正在处理{}发送的命令 {}，参数mode：{}，绑定用户{}",
                sender.getQQ(),
                DataContext.getCommand(),
                arg.getMode(), userRole.getCurrentUname());

        var mode = arg.getMode() == null ? userRole.getMode() : Integer.valueOf(arg.getMode());
        var apiV2Mode = ApiV2ModeHolder.fromInt(mode);

        if (!userRole.isUseLazer()) {
            log.info("用户{}未指定使用Lazer，不处理", userRole.getCurrentUname());
            return;
        }

        var bp = osuApiV2Manager.getUserBestScores(
                new UserScoresRequest(
                        String.valueOf(userRole.getUserId()),
                        100, 0,
                        true,
                        false, apiV2Mode));
        var bpPage2 =osuApiV2Manager.getUserBestScores(
                new UserScoresRequest(
                        String.valueOf(userRole.getUserId()),
                        100, 100,
                        true,
                        false, apiV2Mode));
    }
}
