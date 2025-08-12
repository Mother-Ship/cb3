package top.mothership.cb3.command.processor;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.aop.NeedContextData;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.reflect.CbCmdProcessor;
import top.mothership.cb3.mapper.UserDAO;
import top.mothership.cb3.onebot.websocket.OneBotWebsocketHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class LazerSwitchCommandHandler {

    private final UserDAO userDAO;

    @CbCmdProcessor({"lazer", "stable"})
    @NeedContextData({ContextDataEnum.USER_ROLE})
    public void switchLazer() {
        var sender = DataContext.getSender();
        var userRole = DataContext.getUserRole();
        var command = DataContext.getCommand();
        if (command.equals("lazer")) {
            if (userRole.isUseLazer()) {
                OneBotWebsocketHandler.sendMessage(sender, "你已经在Lazer模式，无需切换");
                log.info("用户{}已处于Lazer模式，无需切换", userRole.getCurrentUname());
                return;
            }


            userRole.setUseLazer(true);
            userDAO.updateUser(userRole);
            log.info("用户{}已切换为Lazer模式", userRole.getCurrentUname());
            OneBotWebsocketHandler.sendMessage(sender, "已切换为Lazer模式");
        } else {
            if (!userRole.isUseLazer()) {
                OneBotWebsocketHandler.sendMessage(sender, "你已经在Stable模式，无需切换");
                log.info("用户{}已处于Stable模式，无需切换", userRole.getCurrentUname());
            }


            userRole.setUseLazer(false);
            userDAO.updateUser(userRole);
            log.info("用户{}已切换为Stable模式", userRole.getCurrentUname());
            OneBotWebsocketHandler.sendMessage(sender, "已切换为Stable模式");
        }

    }
}
