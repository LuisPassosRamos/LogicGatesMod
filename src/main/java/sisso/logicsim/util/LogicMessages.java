package sisso.logicsim.util;

import com.hypixel.hytale.server.core.Message;

/** Let Hytale resolve each recipient's locale and its default-language fallback. */
public final class LogicMessages {
    private LogicMessages() { }
    public static Message text(String key) { return Message.translation("server.logicgates." + key); }
}
