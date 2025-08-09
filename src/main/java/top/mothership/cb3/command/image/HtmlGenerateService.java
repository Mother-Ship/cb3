package top.mothership.cb3.command.image;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import top.mothership.cb3.manager.constant.ApiV2ModeHolder;
import top.mothership.cb3.pojo.osu.apiv2.response.ApiV2Score;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HtmlGenerateService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private ClassPathImageCacheService classPathImageCacheService;

    @Autowired
    private BeatmapCoverImageCacheService beatmapCoverImageCacheService;

    @SneakyThrows
    public String generateResultHtml(ApiV2Score.ScoreLazer score) {
        Context context = new Context();

        // 左上角的谱面数据
        context.setVariable("title", score.getBeatmapset().getTitle());
        context.setVariable("star", score.getBeatmap().getDifficultyRating());
        context.setVariable("artist", score.getBeatmapset().getArtist());

        // 谱面状态的背景在CSS里
        context.setVariable("status", score.getBeatmapset().getStatus());

        context.setVariable("diff", score.getBeatmap().getVersion());

        context.setVariable("mapper", score.getBeatmap().getUser().getUsername());

        // 左侧的分数数据
        context.setVariable("score", score.getScore());

        context.setVariable("acc", score.getAccAuto());
        context.setVariable("maxCombo", score.getMaxCombo());
        context.setVariable("pp", score.getPp());

        switch (score.getMode()) {
            case ApiV2ModeHolder.OSU -> {
                context.setVariable("great", score.getStatistics().getCountGreat());
                context.setVariable("ok", score.getStatistics().getCountOk());
                context.setVariable("meh", score.getStatistics().getCountMeh());
                context.setVariable("miss", score.getStatistics().getCountMiss());
            }
            case ApiV2ModeHolder.TAIKO -> {
                context.setVariable("great", score.getStatistics().getCountGreat());
                context.setVariable("ok", score.getStatistics().getCountOk());
                context.setVariable("miss", score.getStatistics().getCountMiss());
            }
            case ApiV2ModeHolder.FRUITS -> {
                context.setVariable("great", score.getStatistics().getCountGreat());
                context.setVariable("miss", score.getStatistics().getCountMiss());
            }
            case ApiV2ModeHolder.MANIA -> {
                context.setVariable("perfect", score.getStatistics().getCountGeki());
                context.setVariable("great", score.getStatistics().getCountGreat());
                context.setVariable("good", score.getStatistics().getCountKatu());
                context.setVariable("ok", score.getStatistics().getCountOk());
                context.setVariable("meh", score.getStatistics().getCountMeh());
                context.setVariable("miss", score.getStatistics().getCountMiss());
            }
        }
        switch (score.getMode()) {
            case ApiV2ModeHolder.OSU -> {
                context.setVariable("sliderEnd", score.getStatistics().getSliderTailHit());
                context.setVariable("sliderTick", score.getStatistics().getLargeTickHit());
                context.setVariable("spinnerSpin", score.getStatistics().getSmallBonus());
                context.setVariable("spinnerBonus", score.getStatistics().getLargeBonus());
            }

            case ApiV2ModeHolder.FRUITS -> {
                context.setVariable("largeDroplet", score.getStatistics().getLargeTickHit());
                context.setVariable("smallDroplet", score.getStatistics().getSmallTickHit());
            }

        }

        // 左下角MOD和multiplier
        context.setVariable("mods", Arrays.stream(score.getMods())
                .map(ApiV2Score.Mod::getAcronym)
                .collect(Collectors.toSet()));
        var speedChangeMod = Arrays.stream(score.getMods())
                .filter(ApiV2Score.Mod::isSpeedChangeMod)
                .findFirst();
        speedChangeMod.ifPresent(mod -> {
            log.info("解析出变速MOD: {}", mod);
            if (mod.getSettings().has("speed_change")) {
                context.setVariable("mutiplier",
                        mod.getSettings().get("speed_change").asText()
                );
            }
        });
        // 左下角排名、时间
        context.setVariable("globalRank", score.getRankGlobal());
        // 把UTC时区2025-05-09T23:05:38Z格式的endedAt格式化为+8时区的时间，再格式化为2025/02/03 12:34格式
        var endedAtStr = score.getEndedAt();
        endedAtStr = endedAtStr.substring(0, endedAtStr.length() - 1) + "+08:00";
        endedAtStr = ZonedDateTime.parse(endedAtStr)
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
        context.setVariable("endedAt", endedAtStr);


        // 上方banner里的玩家名、模式
        context.setVariable("username", score.getUser().getUsername());
        context.setVariable("mode", score.getMode());

        // 图片
        // 谱面背景（自动下载和缓存，理论上还能做异步加载）
        String imagePath = beatmapCoverImageCacheService.getImage(String.valueOf(score.getBeatmapset().getId()));
        context.setVariable("beatmapCover", imagePath);

        // 玩家头像和Cover
        String playerCover = score.getUser().getCover().getUrl().toString();
        context.setVariable("playerCover", playerCover);
        context.setVariable("avatar", score.getUser().getAvatarUrl().toString());


        // 素材图片
        var rank = score.getRank();
        if (Objects.equals(rank, "SH")) {
            rank = "S";
        }
        if (Objects.equals(rank, "XH")) {
            rank = "X";
        }
        Map<String, String> images = classPathImageCacheService.getImagesDataUrl(
                "lazer-result/images/lazer-banner-" + score.getBeatmap().getMode() + ".png",
                "lazer-result/images/lazer-bg-" + score.getBeatmap().getMode() + ".svg",
                "lazer-result/images/lazer-" + rank.toLowerCase() + ".svg",
                "lazer-result/images/lazer-fc.svg"
        );

        if (score.isPerfectCombo()) {
            context.setVariable("fcIcon", images.get("lazer-result/images/lazer-fc.png"));
        }

        // 添加图片到模板上下文
        context.setVariable("banner", images.get("lazer-result/images/lazer-banner-" + score.getBeatmap().getMode() + ".png"));
        context.setVariable("bg", images.get("lazer-result/images/lazer-bg-" + score.getBeatmap().getMode() + ".svg"));
        context.setVariable("rankIcon", images.get("lazer-result/images/lazer-" + rank.toLowerCase() + ".svg"));


        return templateEngine.process("lazer-result/lazer-result-template", context);
    }

}
