package top.mothership.cb3.command.processor;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.aop.NeedContextData;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.reflect.CbCmdProcessor;

@Component
@Slf4j
@RequiredArgsConstructor
public class LazerSwitchCommandHandler {

    @CbCmdProcessor({"lazer", "stable"})
    @NeedContextData({ContextDataEnum.USER_ROLE})
    public void switchLazer() {
        var sender = DataContext.getSender();
        var userRole = DataContext.getUserRole();
        var command = DataContext.getCommand();
        if (command.equals("lazer")) {
            if (userRole.isUseLazer()) {
                log.info("用户{}已处于Lazer模式，无需切换", userRole.getCurrentUname());
                return;
            }
            userRole.setUseLazer(true);
            log.info("用户{}已切换为Lazer模式", userRole.getCurrentUname());
        } else {
            if (!userRole.isUseLazer()) {
                log.info("用户{}已处于Stable模式，无需切换", userRole.getCurrentUname());
            }
            userRole.setUseLazer(false);
            log.info("用户{}已切换为Stable模式", userRole.getCurrentUname());
        }

    }
}
