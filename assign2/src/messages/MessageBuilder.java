package messages;

import membership.*;
import utils.HashUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MessageBuilder {
    static final String PUT = "PUT";
    static final String GET = "GET";
    static final String DEL = "DELETE";

    Map<String, String> header;
    String body;

    public Map<String, String> getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }

    public MessageBuilder(String msg) {
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

    /**
     * Creates a Join/Leave message with the parameters given
     * @param nodeIP String with the Node IP address
     * @param port Int with the port associated with the node
     * @param membershipCounter Int with the membership counter associated with the given node, <b>even</b> values mean
     *                          it's a <b>join</b> message, <b>odd</b> values mean it's a <b>leave</b> message
     * @return String containing only a header correctly formatted
     */
    public static String joinLeaveMessage(String nodeIP, int port, int membershipCounter, int msPort) {
        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", membershipCounter % 2 == 0 ? "JOIN" : "LEAVE");
        headerLines.put("NodeIP", nodeIP);
        headerLines.put("Port", String.valueOf(port));
        headerLines.put("MembershipCounter", String.valueOf(membershipCounter));
        headerLines.put("MembershipPort", String.valueOf(msPort));

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
    public static String storeMessage(String operation, String key, String value) {
        // BODY
        String body = operation.equalsIgnoreCase(PUT) ? value : "";

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", operation);
        headerLines.put("Key", key);
        headerLines.put("BodySize", String.valueOf(body.length()));

        return buildHeader(headerLines) + body;
    }

    /**
     * Creates a Store Message based on the parameters given, should be used with "GET" and "DELETE"
     * @param operation String with the operation to transmit, could be "GET" or "DELETE"
     * @param key String with the key <b>NOT ENCODED</b>
     * @return String with a header and body correctly formatted
     */
    public static String storeMessage(String operation, String key) {
        return storeMessage(operation, key, null);
    }

    /**
     * Creates a membership message based on the parameters given
     * @param membershipView MembershipView, contains a MembershipTable and a MembershipLog
     * @param nodeIP String with the IP of the sender Node, used for the header only
     * @return String with a header and a body correctly formatted
     */
    public static String membershipMessage(MembershipView membershipView, String nodeIP){
        // BODY
        StringBuilder body = new StringBuilder();
        var msTable = membershipView.getMembershipTable().toString();
        var msLog = new MembershipLog(membershipView.getMembershipLog().last32Logs()).toString();
        body.append(msTable);
        body.append("--sep--\n");
        body.append(msLog);

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", "MEMBERSHIP" );
        headerLines.put("NodeIP", nodeIP);
        headerLines.put("BodySize", String.valueOf(body.toString().length()));

        return buildHeader(headerLines) + body;
    }

    public static MembershipView parseMembershipMessage(MessageBuilder message) {
        MembershipTable membershipTable = new MembershipTable(); // TODO
        MembershipLog membershipLog = new MembershipLog(); // TODO
        String body = message.getBody();
        boolean sep = false;
        Scanner scanner = new Scanner(body);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("--sep--")) sep = true;
            else if (sep) membershipLog.addMembershipInfo(new MembershipLogRecord(line));
            else membershipTable.addMembershipInfo(HashUtils.getHashedSha256(line.trim()), new MembershipInfo(line));

        }
        return new MembershipView(membershipTable, membershipLog);
    }

    public static String clientMessage(String op, String arg) {
        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("NodeIP", "CLIENT");
        headerLines.put("Type", op);
        headerLines.put("BodySize", String.valueOf(arg.length()));

        return buildHeader(headerLines) + arg;
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
            stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
