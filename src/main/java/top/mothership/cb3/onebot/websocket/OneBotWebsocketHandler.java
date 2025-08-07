package top.mothership.cb3.onebot.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.mothership.cb3.onebot.OneBotMessageHandler;
import top.mothership.cb3.onebot.pojo.OneBotApiRequest;
import top.mothership.cb3.onebot.pojo.OneBotApiResponse;
import top.mothership.cb3.onebot.pojo.OneBotEvent;
import top.mothership.cb3.onebot.pojo.OneBotMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OneBotWebsocketHandler extends TextWebSocketHandler {
    //用来保存连接进来session
    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> RESPONSE_MAP = new ConcurrentHashMap<>();
    private final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(100);
    @Autowired
    private OneBotMessageHandler oneBotMessageHandler;
    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    public static String callApi(Long selfId, OneBotApiRequest request) {
        WebSocketSession session = SESSION_MAP.get(String.valueOf(selfId));
        if (session != null) {
            session.sendMessage(new TextMessage(new Gson().toJson(request)));
        }
        // 自旋等待返回值map里出现要的返回值，直到次数上限
        int retry = 0;
        while (true) {
            String response = RESPONSE_MAP.get(request.getEcho());
            if (response != null) {
                return response;
            }
            TimeUnit.SECONDS.sleep(1);

            retry++;
            if (retry > 10) {
                return null;
            }
        }
    }

    @SneakyThrows
    public static void sendMessage(Long selfId, OneBotMessage.OneBotApiParams message) {

        String className = message.getClass().getSimpleName();
        String action = switch (className) {
            case "SendGroupMsgParams" -> "send_group_msg";
            case "SendPrivateMsgParams" -> "send_private_msg";
            case "SetGroupBanParams" -> "set_group_ban";
            case "SetGroupAddRequestParams" -> "set_group_add_request";
            case "SetGroupKickParams" -> "set_group_kick";
            default -> throw new IllegalArgumentException("不支持的参数类型: " + className);
        };

        WebSocketSession session = SESSION_MAP.get(String.valueOf(selfId));
        if (session != null) {
            OneBotApiRequest<OneBotMessage.OneBotApiParams> request = new OneBotApiRequest<>();
            request.setParams(message);
            request.setAction(action);
            session.sendMessage(new TextMessage(new Gson().toJson(request)));
        }
    }

    /**
     * 关闭连接进入这个方法处理，将session从 list中删除
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSION_MAP.remove(getClientQQ(session));
        log.info("{} 连接已经关闭，现从list中删除 ,状态信息{}", session, status);
    }

    /**
     * 三次握手成功，进入这个方法处理，将session 加入list 中
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SESSION_MAP.put(getClientQQ(session), session);
        log.info("用户{}连接成功.... ", session);
    }

    /**
     * 处理客户发送的信息
     */
    @SneakyThrows
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (message.getPayload().toString().contains("echo")
                && !message.getPayload().toString().contains("post_type")) {
            OneBotApiResponse response = objectMapper.readValue(message.getPayload().toString(),
                    OneBotApiResponse.class);
            if (response.getEcho() != null)
                RESPONSE_MAP.put(response.getEcho(), message.getPayload().toString());

        } else {
            OneBotEvent.BaseEvent event = parseEvent(message.getPayload().toString());
            if (event == null) {
                return;
            }
            fixedThreadPool.submit(() -> oneBotMessageHandler.doHandle(event));
        }

    }

    private String getClientQQ(WebSocketSession session) {
        // 按one bot文档，取第一个X-Self-ID请求头
        return session.getHandshakeHeaders().get("X-Self-ID").get(0);
    }

    /**
     * 根据JSON字符串解析成对应的OneBot事件对象
     */
    private OneBotEvent.BaseEvent parseEvent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        String postType = root.path("post_type").asText();

        switch (postType) {
            case "message":
                return objectMapper.treeToValue(root, OneBotEvent.MessageEvent.class);
            case "request":
                String requestType = root.path("request_type").asText();
                if ("group".equals(requestType)) {
                    return objectMapper.treeToValue(root, OneBotEvent.GroupRequestEvent.class);
                }
                break;
            case "notice":
                String noticeType = root.path("notice_type").asText();
                if ("group_increase".equals(noticeType)) {
                    return objectMapper.treeToValue(root, OneBotEvent.GroupIncreaseNoticeEvent.class);
                } else if ("group_decrease".equals(noticeType)) {
                    return objectMapper.treeToValue(root, OneBotEvent.GroupDecreaseNoticeEvent.class);
                }
                break;
            case "meta_event":
                String metaType = root.path("meta_event_type").asText();
                if ("heartbeat".equals(metaType)) {
                    return objectMapper.treeToValue(root, OneBotEvent.HeartbeatMetaEvent.class);
                }
                break;
        }
        log.info("不处理的OneBot事件: {}", json);
        return null;
    }


}
