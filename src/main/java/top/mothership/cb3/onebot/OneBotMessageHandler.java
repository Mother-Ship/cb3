package top.mothership.cb3.onebot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cb3.command.context.DataContext;
import top.mothership.cb3.command.reflect.CbCmdProcessorInfo;
import top.mothership.cb3.command.reflect.CbCmdProcessorRegistry;
import top.mothership.cb3.onebot.pojo.OneBotContextData;
import top.mothership.cb3.onebot.pojo.OneBotEvent;

@Slf4j
@Component
public class OneBotMessageHandler {
    @Autowired
    CbCmdProcessorRegistry registry;

    public void doHandle(OneBotEvent.BaseEvent event) {
        String className = event.getClass().getSimpleName();
        switch (className) {
            case "GroupDecreaseNoticeEvent" -> {
                OneBotEvent.GroupDecreaseNoticeEvent decreaseEvent = (OneBotEvent.GroupDecreaseNoticeEvent) event;
            }
            case "GroupIncreaseNoticeEvent" -> {
                OneBotEvent.GroupIncreaseNoticeEvent increaseEvent = (OneBotEvent.GroupIncreaseNoticeEvent) event;
            }
            case "GroupRequestEvent" -> {
                OneBotEvent.GroupRequestEvent requestEvent = (OneBotEvent.GroupRequestEvent) event;
            }
            case "MessageEvent" -> {
                OneBotEvent.MessageEvent messageEvent = (OneBotEvent.MessageEvent) event;
                // 转义
                parseMessageEventMessage(messageEvent);

                // 查找命令处理器
                CbCmdProcessorInfo processorInfo = registry.getProcessorInfo(messageEvent.getMessage());
                if (processorInfo != null) {
                    log.info("收到类型{}的消息{}，识别为命令，处理器: {}",
                            messageEvent.getMessageType(),
                            messageEvent.getMessage(),
                            processorInfo.getClassName() + "." +
                            processorInfo.getMethodName());

                    // 设置发送者上下文
                    DataContext.setSender(new OneBotContextData(
                            messageEvent.getUserId(),
                            messageEvent.getGroupId(),
                            messageEvent.getSelfId()
                    ));

                    // 反射调用命令处理器
                    registry.invokeProcessor(processorInfo);
                }
            }
        }
    }
    private void parseMessageEventMessage(OneBotEvent.MessageEvent event){
        //转义
        String msg = event.getMessage();
        msg = msg.replaceAll("&#91;", "[");
        msg = msg.replaceAll("&#93;", "]");
        msg = msg.replaceAll("&#44;", ",");
        event.setMessage(msg);
    }
}
