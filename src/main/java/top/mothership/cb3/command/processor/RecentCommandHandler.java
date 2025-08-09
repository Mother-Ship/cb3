package top.mothership.cb3.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.aop.NeedContextData;
import top.mothership.cb3.command.argument.RecentCommandArg;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.image.HtmlGenerateService;
import top.mothership.cb3.command.image.ScreenShotService;
import top.mothership.cb3.command.reflect.CbCmdProcessor;
import top.mothership.cb3.manager.OsuApiV2Manager;
import top.mothership.cb3.manager.constant.ApiV2ModeHolder;
import top.mothership.cb3.onebot.websocket.OneBotWebsocketHandler;
import top.mothership.cb3.pojo.osu.apiv2.request.UserScoresRequest;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;

import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecentCommandHandler {

    private final OsuApiV2Manager osuApiV2Manager;
    private final HtmlGenerateService htmlGeneratorService;
    private final ScreenShotService screenshotService;


    @CbCmdProcessor({"pr", "recent"})
    @NeedContextData({ContextDataEnum.USER_ROLE, ContextDataEnum.BONDED_API_V1_USERINFO})
    public void pr(RecentCommandArg command) {
        var sender = DataContext.getSender();
        var userRole = DataContext.getUserRole();

        log.info("正在处理{}发送的命令 pr，参数mode：{}，绑定用户{}",
                sender.getQQ(), command.getMode(), userRole.getCurrentUname());

        var mode = command.getMode() == null ? userRole.getMode() : Integer.valueOf(command.getMode());
        var apiV2Mode = ApiV2ModeHolder.fromInt(mode);

        if (!userRole.isUseLazer()) {
            log.info("用户{}未指定使用Lazer，不处理", userRole.getCurrentUname());
            return;
        }

        var recent = osuApiV2Manager.getUserRecentScores(
                new UserScoresRequest(
                        String.valueOf(userRole.getUserId()),
                        "recent",
                        100, 0,
                        true,
                        false, apiV2Mode));

        var score = recent.stream().filter(ApiV2Score.ScoreLazer::isLazer).findFirst();
        if (score.isEmpty()) {
            log.info("用户{}没有Lazer最近成绩", userRole.getCurrentUname());
            OneBotWebsocketHandler.sendImage(sender,
                    "玩家" + userRole.getCurrentUname() + "在Lazer模式" + mode + "最近没有游戏记录。");
            return;
        }

        log.info("获取到{}的{}模式Lazer最近成绩：{}", userRole.getCurrentUname(), apiV2Mode, recent);

        // 生成HTML
        String htmlContent = htmlGeneratorService.generateResultHtml(score.get());

        // 转换为图片
        byte[] imageBytes = screenshotService.htmlToImageWithLocalResources(htmlContent);

        // 转base64发送
        var imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

        OneBotWebsocketHandler.sendImage(sender, imageBase64);
    }

}
