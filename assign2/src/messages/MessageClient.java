package messages;

public class MessageClient extends Message{
    public MessageClient(String msg) {
        super(msg);
    }

    public static String storeMessage(String operation, String key, String value) {
        return storeMessage(operation, key, value, true);
    }
    public static String storeMessage(String operation, String key) {
        return storeMessage(operation, key, null, true);
    }
}
