package messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageBuilder {
    static final char CR = 0xD;
    static final char LF = 0xA;
    static final String PUT = "PUT";
    static final String GET = "GET";
    static final String DEL = "DELETE";

    /**
     * Creates a Join/Leave message with the parameters given
     * @param nodeIP String with the Node IP address
     * @param port Int with the port associated with the node
     * @param membershipCounter Int with the membership counter associated with th given node, even values mean
     *                          it's a join message, odd values mean it's a leave message
     * @return String containing only a header correctly formatted
     */
    public static String messageJoinLeave(String nodeIP, int port, int membershipCounter) {

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("NodeIP", nodeIP);
        headerLines.put("Port", String.valueOf(port));
        headerLines.put("MembershipCounter", String.valueOf(membershipCounter));

        // NO BODY IN JOIN/LEAVE MESSAGES!!

        return buildHeader(headerLines);
    }

    /**
     * Creates a Store Message based on the parameters given
     * @param operation String with the operation to transmit, could be "PUT", "GET" or "DELETE"
     * @param key String with the key *NOT ENCODED*
     * @param value String with the value associated with the key *NOT ENCODED*
     * @return String with a header and body correctly formatted
     */
    public static String messageStore(String operation, String key, String value) {

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Operation", operation);
        headerLines.put("Key", key);

        StringBuilder message = new StringBuilder().append(buildHeader(headerLines));

        // BODY
        if(operation.equals(PUT))
            message.append(value);
        else {
            message.append("NO VALUE IN ");
            message.append(operation);
            message.append(" MESSAGES");
        }

        return message.toString();
    }

    /**
     * Creates the header for the message to send based on a list of lines
     * @param headerLines List of Strings with the lines to append to the header
     * @return String with the header formatted with the given lines
     */
    private static String buildHeader(Map<String, String> headerLines) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : headerLines.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append(": ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append(CR);
            stringBuilder.append(LF);
        }
        stringBuilder.append(CR);
        stringBuilder.append(LF);
        return stringBuilder.toString();
    }

}
