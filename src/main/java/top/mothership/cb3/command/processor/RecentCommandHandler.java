package top.mothership.cb3.command.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.aop.NeedContextData;
import top.mothership.cb3.command.argument.RecentCommandArg;
import top.mothership.cb3.command.constant.ContextDataEnum;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.reflect.CbCmdProcessor;

@Component
@Slf4j
public class RecentCommandHandler {
    @CbCmdProcessor({"pr","recent"})
    @NeedContextData({ContextDataEnum.USER_ROLE, ContextDataEnum.API_V1_USERINFO})
    public void pr(RecentCommandArg command){
        var sender = DataContext.getSender();
        log.info("正在处理命令 pr，参数mode：{}",command.getMode());

    }
}
