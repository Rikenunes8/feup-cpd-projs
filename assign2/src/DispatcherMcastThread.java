import membership.MembershipView;
import messages.MessageStore;
import messages.TcpMessager;

import java.io.IOException;
import java.util.HashSet;
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
                default -> System.out.println("Type case not implemented");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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

        Thread.sleep(new Random().nextInt(MAX_WAIT)); // Wait random time before send membership message

        String msMsg = MessageStore.membershipMessage(this.store.getId(), this.store.getMembershipView());
        try { TcpMessager.sendMessage(nodeIP, msPort, msMsg); }
        catch (IOException e) { System.out.println("Socket is already closed: " + e.getMessage()); }

        transferOwnership(nodeID);
    }

    private void leaveHandler(Map<String, String> header) {
        System.out.println("Received leave message from " + header.get("NodeIP"));
        String nodeID = header.get("NodeID");
        String nodeIP = header.get("NodeIP");
        int storePort = Integer.parseInt(header.get("StorePort"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));
        this.store.updateMembershipView(nodeID, nodeIP, storePort, msCounter);
    }

    private void membershipHandler(MessageStore message) {
        System.out.println("Receive membership message from " + message.getHeader().get("NodeID"));
        MembershipView view = parseMembershipMessage(message);
        this.store.updateMembershipView(view.getMembershipTable(), view.getMembershipLog());
    }

    private void transferOwnership(String nodeID) {
        if (this.store.getClusterSize() <= 1) return;
        var keysCopy = new HashSet<>(this.store.getKeys());
        for (String key : keysCopy) {
            var preferenceList = this.store.getPreferenceList(key);
            // If nodes are less than replication factor and this node was the closest before new node arrival copy file
            if (this.store.getClusterSize() <= Store.REPLICATION_FACTOR) {
                var prevClosestNode = preferenceList.stream()
                        .filter(nodeKey -> !nodeKey.equals(nodeID)).findFirst().get();
                if (prevClosestNode.equals(this.store.getId())) {
                    this.store.transfer(nodeID, key, false);
                }
            }
            else {
                if (!preferenceList.contains(nodeID) || preferenceList.contains(this.store.getId()))
                    continue;
                if (this.store.getKeys().contains(key) && !preferenceList.contains(this.store.getId())) {
                    this.store.transfer(nodeID, key, true);
                }
            }
        }
    }
}
