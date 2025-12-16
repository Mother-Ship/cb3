package top.mothership.cb3.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    public void checkMP5Card(@RequestParam String outputOnly) {
        var mp5GroupMemberList = OneBotWebsocketHandler.getGroupMembers(136312506L);
        StringBuilder sb = new StringBuilder();

        var replyMessage = new OneBotMessage.SendGroupMsgParams();
        replyMessage.setGroupId(724149648L);


        for (var qqInfo : mp5GroupMemberList.getData()) {
            if (qqInfo.getUserId() == 1335734629L
                    || qqInfo.getUserId() == 1020640876L
                    || qqInfo.getUserId() == 3145729213L){
                log.warn("QQ {} 在群 {} 是bot，跳过", qqInfo.getUserId(), qqInfo.getGroupId());
                continue;
            }
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
                if (StringUtils.isBlank(card)) {
                    log.warn("QQ {} 在群 {} 名片为空，判断昵称", qqInfo.getUserId(), qqInfo.getGroupId());
                    // 如果名片为空，则判断昵称
                    if (!isNicknameMatchUsername(qqInfo.getNickname(), currentUsername)) {
                        sb.append("QQ：").append(qqInfo.getUserId())
                                .append("的群名片为空，且id和昵称不一致，osu! id：")
                                .append(currentUsername)
                                .append("，昵称：").append(qqInfo.getNickname()).append("，已修改名片为ID\n");
                        log.info("QQ {} 在群 {} 名片为空，且昵称不包含ID，触发修改名片", qqInfo.getUserId(), qqInfo.getGroupId());
                        if (!Objects.equals(outputOnly, "true")) {
                            OneBotWebsocketHandler.setGroupCard(qqInfo.getGroupId(), qqInfo.getUserId(), currentUsername);
                        }
                    }
                } else {
                    if (!isCardMatchUsername(card, currentUsername)) {
                        sb.append("QQ：").append(qqInfo.getUserId())
                                .append("的id和名片不一致，osu! id：")
                                .append(currentUsername)
                                .append("，群名片：").append(card).append("，已修改为ID\n");
                        log.info("QQ {} 在群 {} 名片不包含ID，触发修改名片", qqInfo.getUserId(), qqInfo.getGroupId());
                        if (!Objects.equals(outputOnly, "true")) {
                            OneBotWebsocketHandler.setGroupCard(qqInfo.getGroupId(), qqInfo.getUserId(), currentUsername);
                        }
                    }
                }
            } else {
                sb.append("QQ： ").append(qqInfo.getUserId()).append(" 没有绑定id，群名片是：").append(card)
                        .append("，昵称是").append(qqInfo.getNickname()).append("\n");
                log.info("QQ {} 在群 {} 没有绑定id，触发绑定提醒", qqInfo.getUserId(), qqInfo.getGroupId());
                var remindMessage = new OneBotMessage.SendGroupMsgParams();
                remindMessage.setGroupId(136312506L);
                remindMessage.setMessage("[CQ:at,qq=" + qqInfo.getUserId() + "]" +
                        " 你好，你还没有绑定你的osu账号哦，请输入!setid 你的osu!用户名 来绑定，例如：!setid MotherShip，注意用户名前面有一个空格，谢谢配合~");
                if (!Objects.equals(outputOnly, "true")) {
                    OneBotWebsocketHandler.sendMessage(1335734629L, remindMessage);
                }

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