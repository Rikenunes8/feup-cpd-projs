package messages;

public class MessageClient extends Message{
    public MessageClient(String msg) {
        super(msg);
    }

    public static String putMessage(String key, String value) {
        return Message.putMessage(key, value);
    }
    public static String getMessage(String key) {
        return Message.getMessage(key);
    }
    public static String deleteMessage(String key) {
        return Message.deleteMessage(key);
    }
}
