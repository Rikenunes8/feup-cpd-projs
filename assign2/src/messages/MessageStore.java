package messages;

import membership.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private static String joinLeaveMessage(String type, String nodeID, String nodeIP, Integer port, Integer msPort, Integer msCounter, boolean all) {
        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", type);
        headerLines.put("NodeID", nodeID);
        headerLines.put("NodeIP", nodeIP);
        headerLines.put("StorePort", String.valueOf(port));
        headerLines.put("MembershipCounter", String.valueOf(msCounter));
        if (msPort != null) headerLines.put("MembershipPort", String.valueOf(msPort));
        if (all) headerLines.put("All", "true");

        // NO BODY IN JOIN/LEAVE MESSAGES!!

        return buildHeader(headerLines);
    }

    public static String joinMessage(String nodeID, String nodeIP, int storePort, int membershipCounter, int msPort) {
        return joinLeaveMessage("JOIN", nodeID, nodeIP, storePort, msPort, membershipCounter, false);
    }
    public static String leaveMessage(String nodeID, String nodeIP, int storePort, int membershipCounter) {
        return joinLeaveMessage("LEAVE", nodeID, nodeIP, storePort, null, membershipCounter, false);
    }
    public static String msUpdateMessage(String nodeID, String nodeIP, int storePort, int membershipCounter, int msPort, boolean all) {
        return joinLeaveMessage("MS_UPDATE", nodeID, nodeIP, storePort, msPort, membershipCounter, all);
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
    public static String replicaPutMessage(String key, String value) {
        return storeMessage("REPLICA_PUT", key, value);
    }
    public static String replicaDelMessage(String key) {
        return storeMessage("REPLICA_DEL", key, null);
    }
    public static String replicaGetMessage(String key) {
        return storeMessage("REPLICA_GET", key, null);
    }



    /**
     * Creates a membership message based on the parameters given
     * @param membershipView MembershipView, contains a MembershipTable and a MembershipLog
     * @param nodeID String with the ID of the sender Node, used for the header only
     * @return String with a header and a body correctly formatted
     */
    public static String membershipMessage(String nodeID, MembershipView membershipView){
       return membershipMessage(nodeID, membershipView, false);
    }
    public static String membershipMessage(String nodeID, MembershipView membershipView, boolean allLogs){
        // BODY
        StringBuilder body = new StringBuilder();
        var msTable = membershipView.getMembershipTable().toString();
        var msLog = allLogs
                ? new MembershipLog(membershipView.getMembershipLog().getLogs()).toString()
                : new MembershipLog(membershipView.getMembershipLog().last32Logs()).toString();
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
                var idInfo = line.split("\\|", 2);
                membershipTable.addMembershipInfo(idInfo[1], new MembershipInfo(idInfo[0]));
            }
        }
        return new MembershipView(membershipTable, membershipLog);
    }

    public static String pendingRequestsMessage(Map<String, AbstractQueue<String>> pendingRequests) {
        // BODY
        StringBuilder body = new StringBuilder();
        for (String key : pendingRequests.keySet()) {
            body.append("$KEY$").append(key);
            for (var message : pendingRequests.get(key)) {
                body.append("$MSG$").append(message);
            }
        }

        // Setting up the header
        Map<String, String> headerLines = new HashMap<>();
        headerLines.put("Type", "REQUESTS" );
        headerLines.put("BodySize", String.valueOf(body.toString().length()));

        return buildHeader(headerLines) + body;
    }

    public static Map<String, AbstractQueue<String>> parsePendingRequestsMessage(Message message) {
        Map<String, AbstractQueue<String>> map = new HashMap<>();
        String body = message.getBody();
        var keysSections = body.split("\\$KEY\\$");
        for (var aux : keysSections) {
            var aux2 = aux.split("\\$MSG\\$");
            if (aux2.length < 2)  continue;
            String key = aux2[0];
            AbstractQueue<String> msgs = new ConcurrentLinkedQueue<>();
            for (int i = 1; i < aux2.length; i++) {
                msgs.add(aux2[i]);
            }
            map.put(key, msgs);
        }
        return map;
    }
}
