package store;

import membership.MembershipView;
import messages.MessageStore;
import messages.TcpMessager;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import static messages.MessageStore.parseMembershipMessage;

public class DispatcherMcastThread implements Runnable {
    private final int MAX_WAIT = 500;

    private final Store store;
    private final String msgString;

    public DispatcherMcastThread(Store store, String msgString) {
        this.store = store;
        this.msgString = msgString;
    }

    @Override
    public void run() {
        MessageStore message = new MessageStore(this.msgString);
        try {
            Map<String, String> header = message.getHeader();
            switch (header.get("Type")) {
                case "JOIN" -> this.joinHandler(header);
                case "LEAVE" -> this.leaveHandler(header);
                case "MEMBERSHIP" -> this.membershipHandler(message);
                case "MS_UPDATE" -> this.msUpdateHandler(header, header.containsKey("All"));
                default -> System.out.println("Type case not implemented");
            }
        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    private void joinHandler(Map<String, String> header) throws InterruptedException {
        System.out.println("Received join message from " + header.get("NodeIP"));
        String nodeID = header.get("NodeID");
        String nodeIP = header.get("NodeIP");
        int storePort = Integer.parseInt(header.get("StorePort"));
        int msPort    = Integer.parseInt(header.get("MembershipPort"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));

        this.store.updateMembershipView(nodeID, nodeIP, storePort, msCounter);

        if (this.store.getId().equals(nodeID)) return; // Must ignore a join message from itself
        if (nodeID.equals(this.store.getLastSent())) return; // Must ignore a join message when it already sends the MS view, and it wasn't update in meanwhile
        this.store.setLastSent(nodeID);

        if (this.store.shouldSendMembershipMessage() || this.store.getClusterSize() < MembershipCollector.MAX_MS_MSG) {
            Thread.sleep(new Random().nextInt(MAX_WAIT)); // Wait random time before send membership message

            String msMsg = MessageStore.membershipMessage(this.store.getId(), this.store.getMembershipView());
            try { TcpMessager.sendMessage(nodeIP, msPort, msMsg); }
            catch (IOException e) { System.out.println("Socket is already closed: " + e.getMessage()); }
        }

        this.store.transferOwnershipOnJoin(nodeID);
        this.store.emptyPendingRequests(nodeID);
    }

    private void leaveHandler(Map<String, String> header) {
        System.out.println("Received leave message from " + header.get("NodeIP"));
        String nodeID = header.get("NodeID");
        String nodeIP = header.get("NodeIP");
        int storePort = Integer.parseInt(header.get("StorePort"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));
        this.store.updateMembershipView(nodeID, nodeIP, storePort, msCounter);

        this.store.emptyPendingRequests(nodeID);
    }

    private void membershipHandler(MessageStore message) {
        String nodeId = message.getHeader().get("NodeID");
        System.out.println("Receive membership message from " + nodeId);
        MembershipView view = parseMembershipMessage(message);
        this.store.updateMembershipView(view.getMembershipTable(), view.getMembershipLog());

        this.store.selectAlarmer(false, nodeId);
        this.store.applyPendingRequests(nodeId);
    }

    private void msUpdateHandler(Map<String, String> header, boolean all) throws InterruptedException {
        System.out.println("Received membership update message from " + header.get("NodeIP"));
        String nodeID = header.get("NodeID");
        String nodeIP = header.get("NodeIP");
        int msPort    = Integer.parseInt(header.get("MembershipPort"));

        if (this.store.getId().equals(nodeID)) return; // Must ignore a msUpdate message from itself

        if (this.store.shouldSendMembershipMessage() || this.store.getClusterSize() < MembershipCollector.MAX_MS_MSG) {
            Thread.sleep(new Random().nextInt(MAX_WAIT)); // Wait random time before send membership message

            String msMsg = MessageStore.membershipMessage(this.store.getId(), this.store.getMembershipView(), all);
            try {TcpMessager.sendMessage(nodeIP, msPort, msMsg);}
            catch (IOException e) {System.out.println("Socket is already closed: " + e.getMessage());}
        }
        if (!all) this.store.applyPendingRequests(nodeID);
    }
}
