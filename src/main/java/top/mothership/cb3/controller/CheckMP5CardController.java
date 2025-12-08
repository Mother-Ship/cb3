package top.mothership.cb3.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cb3.manager.OsuApiV1Manager;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.onebot.pojo.OneBotMessage;
import top.mothership.cb3.onebot.websocket.OneBotWebsocketHandler;
import top.mothership.cb3.pojo.domain.UserRoleEntity;
import top.mothership.cb3.pojo.osu.apiv1.ApiV1UserInfoVO;
import top.mothership.cb3.util.UserRoleDataUtil;

import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/check")
@Slf4j
@RequiredArgsConstructor
public class CheckMP5CardController {

    private final UserDAO userDAO;
    private final OsuApiV1Manager osuApiV1Manager;
    private final UserRoleDataUtil userRoleDataUtil;

    @PostMapping("/trigger")
    public void checkMP5Card(@RequestParam String debug) {
        var mp5GroupMemberList = OneBotWebsocketHandler.getGroupMembers(136312506L);
        StringBuilder sb = new StringBuilder();

        var replyMessage = new OneBotMessage.SendGroupMsgParams();
        replyMessage.setGroupId(136312506L);


        if (Objects.equals(debug, "true")){
            replyMessage = new OneBotMessage.SendGroupMsgParams();
            replyMessage.setGroupId(724149648L);
        }


        for (var qqInfo : mp5GroupMemberList.getData()) {
            //根据QQ获取user和群名片
            UserRoleEntity userRoleEntity = userDAO.getUser(qqInfo.getUserId(), null);
            String card = qqInfo.getCard();

            if (userRoleEntity != null) {
                // 调用API，如果改名则更新UserRole表
                ApiV1UserInfoVO userinfo = osuApiV1Manager.getUserInfo(0, userRoleEntity.getUserId());
                if (userinfo == null) {
                    userRoleEntity.setBanned(true);
                    userDAO.updateUser(userRoleEntity);
                } else {
                    userRoleEntity.setBanned(false);
                    if (!userinfo.getUsername().equals(userRoleEntity.getCurrentUname())) {
                        userRoleEntity = userRoleDataUtil.renameUser(userRoleEntity, userinfo.getUsername());
                    }
                    userDAO.updateUser(userRoleEntity);
                }

                // 统一使用UserRole表里的用户名进行比较
                String currentUsername = userRoleEntity.getCurrentUname();
                if (card == null) {
                    log.warn("QQ {} 在群 {} 名片为空，判断昵称", qqInfo.getUserId(), qqInfo.getGroupId());
                    // 如果名片为空，则判断昵称
                    if (!isNicknameMatchUsername(qqInfo.getNickname(), currentUsername)) {
                        sb.append("QQ：").append(qqInfo.getUserId())
                                .append("的id和昵称不一致，osu! id：")
                                .append(currentUsername)
                                .append("，昵称：").append(qqInfo.getNickname()).append("\n");
                    }
                } else {
                    if (!isCardMatchUsername(card, currentUsername)) {
                        sb.append("QQ：").append(qqInfo.getUserId())
                                .append("的id和名片不一致，osu! id：")
                                .append(currentUsername)
                                .append("，群名片：").append(card).append("\n");
                    }
                }
            } else {
                sb.append("QQ： ").append(qqInfo.getUserId()).append(" 没有绑定id，群名片是：").append(card)
                        .append("，昵称是").append(qqInfo.getNickname()).append("\n");
            }

        }
        replyMessage.setMessage(sb.toString());
        log.info(sb.toString());

        OneBotWebsocketHandler.sendMessage(1335734629L, replyMessage);
    }

    /**
     * 判断群名片是否与osu!用户名匹配
     *
     * @param card     群名片
     * @param username osu!用户名
     * @return 是否匹配
     */
    private boolean isCardMatchUsername(String card, String username) {
        if (card == null || username == null) {
            return false;
        }
        return card.toLowerCase(Locale.CHINA).replace("_", " ")
                .contains(username.toLowerCase(Locale.CHINA).replace("_", " "));
    }

    /**
     * 判断群昵称是否与osu!用户名匹配（当名片为空时使用）
     *
     * @param nickname 群昵称
     * @param username osu!用户名
     * @return 是否匹配
     */
    private boolean isNicknameMatchUsername(String nickname, String username) {
        if (nickname == null || username == null) {
            return false;
        }
        return nickname.toLowerCase(Locale.CHINA).replace("_", " ")
                .contains(username.toLowerCase(Locale.CHINA).replace("_", " "));
    }
}