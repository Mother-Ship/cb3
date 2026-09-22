package top.mothership.cb3.manager;

/**
 * osu! API 暂时不可用（限流 429、5xx、网络异常、鉴权失败等）。
 *
 * <p>这个异常用来把“接口现在查不了”和“接口查得到、但结果为空（用户不存在/被封禁）”区分开。
 * 旧代码在请求失败时返回 null，调用方（{@code ImportTask}、{@code CheckMP5CardController}）
 * 会把 null 当成“玩家被封禁”，于是一次限流就会把大量正常玩家标记为 banned。
 * 因此请求失败必须抛该异常，绝不能被当作玩家封禁处理。</p>
 */
public class OsuApiUnavailableException extends RuntimeException {

    public OsuApiUnavailableException(String message) {
        super(message);
    }

    public OsuApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
