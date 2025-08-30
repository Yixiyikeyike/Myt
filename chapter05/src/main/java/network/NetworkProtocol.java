package network;

public class NetworkProtocol {
    // 消息类型
    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_MOVE = "MOVE";
    public static final String TYPE_GAME_START = "GAME_START";
    public static final String TYPE_GAME_OVER = "GAME_OVER";
    public static final String TYPE_ERROR = "ERROR";

    // 分隔符
    public static final String DELIMITER = "|";

    public static String createMessage(String type, String data) {
        return type + DELIMITER + data;
    }

    public static String[] parseMessage(String message) {
        if (message == null || !message.contains(DELIMITER)) {
            return new String[]{TYPE_ERROR, "Invalid message"};
        }
        return message.split("\\" + DELIMITER, 2);
    }
}