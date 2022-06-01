package messages;

import java.util.HashMap;
import java.util.Map;

public class Message {
    String message;
    Map<String, String> header;
    String body;

    public Map<String, String> getHeader() {
        return header;
    }
    public String getBody() {
        return body;
    }
    public String getAllMessage() {
        return message;
    }

    public Message(String msg) {
        this.message = msg;

        String[] msgArr = msg.split("\n\n", 2);

        String header = msgArr[0];
        String[] headerLines = header.split("\n");
        this.header = new HashMap<>();
        for (String headerLine : headerLines) {
            String[] keyVal = headerLine.split(": ");
            if (keyVal.length < 2) continue;
            this.header.put(keyVal[0], keyVal[1]);
        }
        this.body = msgArr.length < 2 ?  "" : msgArr[1];
    }

    /**
     * Creates a Store Message based on the parameters given, <b>should only be used with "PUT" operation</b>
     * @param operation String with the operation to transmit, <b>should be "PUT"</b>
     * @param key String with the key <b>NOT ENCODED</b>
     * @param value String with the value associated with the key <b>NOT ENCODED</b>
     * @return String with a header and body correctly formatted
     */
    protected static String storeMessage(String operation, String key, String value) {
        // BODY
        String body = value != null ? value : "";

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", operation);
        headerLines.put("Key", key);
        headerLines.put("BodySize", String.valueOf(body.length()));

        return buildHeader(headerLines) + body;
    }
    protected static String putMessage(String key, String value) {
        return storeMessage("PUT", key, value);
    }
    protected static String getMessage(String key) {
        return storeMessage("GET", key, null);
    }
    protected static String deleteMessage(String key) {
        return storeMessage("DELETE", key, null);
    }


    public static String simpleMessage(String op, String arg) {
        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", op);
        headerLines.put("BodySize", String.valueOf(arg.length()));

        return buildHeader(headerLines) + arg;
    }

    public static String ackMessage(String body) {
        return simpleMessage("ACK", body);
    }

    /**
     * Creates the header for the message to send based on a list of lines
     * @param headerLines List of Strings with the lines to append to the header
     * @return String with the header formatted with the given lines
     */
    protected static String buildHeader(Map<String, String> headerLines) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : headerLines.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append(": ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
