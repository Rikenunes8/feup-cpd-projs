package messages;

import java.util.HashMap;
import java.util.Map;

public class Message {
    static final String PUT = "PUT";
    static final String GET = "GET";
    static final String DEL = "DELETE";

    String message;
    Map<String, String> header;
    String body;

    public Map<String, String> getHeader() {
        return header;
    }
    public String getBody() {
        return body;
    }
    public String getMessage() {
        return message;
    }

    public Message(String msg) {
        this.message = msg;
        // TODO
        System.out.println("\n--- MESSAGE ---");
        System.out.println(msg); // DEBUG
        System.out.println("--- END MESSAGE ---\n");

        String[] msgArr = msg.split("\n\n", 2);

        String header = msgArr[0];
        String[] headerLines = header.split("\n");
        this.header = new HashMap<>();
        for (String headerLine : headerLines) {
            String[] keyVal = headerLine.split(": ");
            this.header.put(keyVal[0], keyVal[1]);
        }
        this.body = msgArr.length < 2 ?  "" : msgArr[1];
    }

    protected static String storeMessage(String operation, String key, String value, boolean client) {
        // BODY
        String body = operation.equalsIgnoreCase(PUT) ? value : "";

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", operation);
        headerLines.put("Key", key);
        headerLines.put("BodySize", String.valueOf(body.length()));
        headerLines.put("From", client ? "client" : "store");

        return buildHeader(headerLines) + body;
    }


    public static String simpleMessage(String op, String arg) {
        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", op);
        headerLines.put("BodySize", String.valueOf(arg.length()));

        return buildHeader(headerLines) + arg;
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
