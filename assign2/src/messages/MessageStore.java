package messages;

import membership.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MessageStore extends Message{
    public MessageStore(String msg) {
        super(msg);
    }

    /**
     * Creates a Join/Leave message with the parameters given
     * @param nodeIP String with the Node IP address
     * @param port Int with the port associated with the node
     * @param msCounter Int with the membership counter associated with the given node, <b>even</b> values mean
     *                          it's a <b>join</b> message, <b>odd</b> values mean it's a <b>leave</b> message
     * @return String containing only a header correctly formatted
     */
    private static String joinLeaveMessage(String nodeID, String nodeIP, int port, Integer msPort, int msCounter, String type) {
        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", type);
        headerLines.put("NodeID", nodeID);
        headerLines.put("NodeIP", nodeIP);
        headerLines.put("StorePort", String.valueOf(port));
        headerLines.put("MembershipCounter", String.valueOf(msCounter));
        if (msPort != null) headerLines.put("MembershipPort", String.valueOf(msPort));

        // NO BODY IN JOIN/LEAVE MESSAGES!!

        return buildHeader(headerLines);
    }

    public static String joinMessage(String nodeID, String nodeIP, int storePort, int membershipCounter, int msPort) {
        return joinLeaveMessage(nodeID, nodeIP, storePort, msPort, membershipCounter, "JOIN");
    }
    public static String leaveMessage(String nodeID, String nodeIP, int storePort, int membershipCounter) {
        return joinLeaveMessage(nodeID, nodeIP, storePort, null, membershipCounter, "LEAVE");
    }
    public static String rejoinMessage(String nodeID, String nodeIP, int storePort, int membershipCounter, int msPort) {
        return joinLeaveMessage(nodeID, nodeIP, storePort, msPort, membershipCounter, "REJOIN");
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
    public static String replicaMessage(String key, String value) {
        return storeMessage("REPLICA", key, value);
    }
    public static String delReplicaMessage(String key) {
        return storeMessage("DEL_REPLICA", key, null);
    }


    /**
     * Creates a membership message based on the parameters given
     * @param membershipView MembershipView, contains a MembershipTable and a MembershipLog
     * @param nodeID String with the ID of the sender Node, used for the header only
     * @return String with a header and a body correctly formatted
     */
    public static String membershipMessage(String nodeID, MembershipView membershipView){
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
        headerLines.put("NodeID", nodeID);
        headerLines.put("BodySize", String.valueOf(body.toString().length()));

        return buildHeader(headerLines) + body;
    }

    public static MembershipView parseMembershipMessage(Message message) {
        MembershipTable membershipTable = new MembershipTable();
        MembershipLog membershipLog = new MembershipLog();
        String body = message.getBody();
        boolean sep = false;
        Scanner scanner = new Scanner(body);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("--sep--")) sep = true;
            else if (sep) membershipLog.addMembershipInfo(new MembershipLogRecord(line));
            else {
                var idInfo = line.split(":", 2);
                membershipTable.addMembershipInfo(idInfo[0], new MembershipInfo(idInfo[1]));
            }
        }
        return new MembershipView(membershipTable, membershipLog);
    }
}
