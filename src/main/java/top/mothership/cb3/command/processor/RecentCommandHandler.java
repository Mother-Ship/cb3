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
import top.mothership.cb3.manager.OsuApiV1Manager;
import top.mothership.cb3.manager.OsuApiV2Manager;
import top.mothership.cb3.manager.constant.ApiV2ModeHolder;
import top.mothership.cb3.onebot.websocket.OneBotWebsocketHandler;
import top.mothership.cb3.pojo.osu.apiv2.request.UserScoresRequest;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2User;

import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecentCommandHandler {

    private final OsuApiV2Manager osuApiV2Manager;
    private final HtmlGenerateService htmlGeneratorService;
    private final ScreenShotService screenshotService;
    private final OsuApiV1Manager osuApiV1Manager;


    @CbCmdProcessor({"pr", "recent"})
    @NeedContextData({ContextDataEnum.USER_ROLE, ContextDataEnum.BONDED_API_V2_USERINFO})
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

        var scoreOp = recent.stream().filter(ApiV2Score.ScoreLazer::isLazer).findFirst();
        if (scoreOp.isEmpty()) {
            log.info("用户{}没有Lazer最近成绩", userRole.getCurrentUname());
            OneBotWebsocketHandler.sendMessage(sender,
                    "玩家" + userRole.getCurrentUname() + "在Lazer的模式" + apiV2Mode + "最近没有游戏记录。");
            return;
        }

        log.info("获取到{}的{}模式Lazer最近成绩：{}", userRole.getCurrentUname(), apiV2Mode, recent);

        // 补全谱面难度的作者名
        var recentScore = scoreOp.get();
        var creatorId = recentScore.getBeatmap().getUserId();
        var dummyMapper = new ApiV2User.User();

        var userInfo = osuApiV1Manager.getUserInfo(0, String.valueOf(creatorId));
        if (userInfo != null) {
            dummyMapper.setUsername(userInfo.getUsername());

        } else {
            log.warn("获取谱面作者信息失败，使用set作者兜底");
            dummyMapper.setUsername(recentScore.getBeatmapset().getCreator());
        }
        recentScore.getBeatmap().setUser(dummyMapper);

        // 补全用户cover
        var apiV2User = DataContext.getApiV2User();
        recentScore.getUser().setCover(apiV2User.getCover());

        // 生成HTML
        String htmlContent = htmlGeneratorService.generateResultHtml(recentScore);

        // 转换为图片
        byte[] imageBytes = screenshotService.htmlToImageWithLocalResources(htmlContent);

        // 转base64发送
        var imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

        OneBotWebsocketHandler.sendImage(sender, imageBase64);
    }

}
