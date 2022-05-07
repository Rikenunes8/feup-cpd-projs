package messages;

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
     * @param membershipCounter Int with the membership counter associated with the given node, <b>even</b> values mean
     *                          it's a <b>join</b> message, <b>odd</b> values mean it's a <b>leave</b> message
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
     * Creates a Store Message based on the parameters given, <b>should only be used with "PUT" operation</b>
     * @param operation String with the operation to transmit, <b>should be "PUT"</b>
     * @param key String with the key <b>NOT ENCODED</b>
     * @param value String with the value associated with the key <b>NOT ENCODED</b>
     * @return String with a header and body correctly formatted
     */
    public static String messageStore(String operation, String key, String value) {

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Operation", operation);
        headerLines.put("Key", key);

        StringBuilder message = new StringBuilder().append(buildHeader(headerLines));

        // BODY ?
        if(operation.equalsIgnoreCase(PUT))
            message.append(value);

        return message.toString();
    }

    /**
     * Creates a Store Message based on the parameters given, should be used with "GET" and "DELETE"
     * @param operation String with the operation to transmit, could be "GET" or "DELETE"
     * @param key String with the key <b>NOT ENCODED</b>
     * @return String with a header and body correctly formatted
     */
    public static String messageStore(String operation, String key) {

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Operation", operation);
        headerLines.put("Key", key);

        // NO BODY

        return buildHeader(headerLines);
    }

    /**
     * Creates a membership message based on the parameters given
     * @param membershipLog MembershipLog with a max size of 32, containing the most recent membership log info
     * @param membershipTable MembershipTable, contains information about all the nodes in current use according
     *                        to a specific node
     * @param sendNodeIP String with the IP of the sender Node, used for the header only
     * @return String with a header and a body correctly formatted
     */
    public static String membershipMessage(MembershipLog membershipLog, MembershipTable membershipTable,
                                           String sendNodeIP){

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("NodeIP", sendNodeIP);

        StringBuilder message = new StringBuilder().append(buildHeader(headerLines));

        // BODY
        message.append(membershipTable.toString());
        message.append(membershipLog.toString());

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
