package top.mothership.cb3.command.context;

import top.mothership.cb3.command.argument.Sender;

public class DataContext {
    private static final ThreadLocal<Sender> sender = new ThreadLocal<>();

    public static void setSender(Sender senderInfo) {
        sender.set(senderInfo);
    }

    public static Sender getSender() {
        return sender.get();
    }

    public static void clear() {
        sender.remove();
    }

}
