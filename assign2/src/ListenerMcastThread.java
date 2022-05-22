
import membership.MembershipView;
import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import static messages.MessageBuilder.parseMembershipMessage;
import static messages.MulticastMessager.receiveMcastMessage;

public class ListenerMcastThread implements Runnable {
    private final int MAX_WAIT = 500;
    private final Store store;

    public ListenerMcastThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        // ExecutorService executor = Executors.newFixedThreadPool(4);
        while (this.store.getMembershipCounter() % 2 == 0) {
            try {
                String msg = receiveMcastMessage(this.store.getRcvDatagramSocket());
                MessageBuilder message = new MessageBuilder(msg);
                
                Map<String, String> header = message.getHeader();
                switch (header.get("Type")) {
                    case "JOIN" -> this.joinHandler(header);
                    case "LEAVE" -> this.leaveHandler(header);
                    case "MEMBERSHIP" -> this.membershipHandler(message);
                    default -> System.out.println("Type case not implemented");
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
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

        if (this.store.getId().equals(nodeID)) return;

        Thread.sleep(new Random().nextInt(MAX_WAIT)); // Wait random time before send membership message

        String msMsg = MessageBuilder.membershipMessage(this.store.getId(), this.store.getMembershipView());
        try { TcpMessager.sendMessage(nodeIP, msPort, msMsg); } // TODO Should not resend if no changes since last time
        catch (IOException e) { System.out.println("ERROR: " + e.getMessage()); }

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

    private void membershipHandler(MessageBuilder message) {
        System.out.println("Multicast Membership Message received");
        MembershipView view = parseMembershipMessage(message);
        this.store.updateMembershipView(view.getMembershipTable(), view.getMembershipLog());
    }

    private void transferOwnership(String nodeID) {
        var keysCopy = new HashSet<>(this.store.getKeys());

        if (this.store.getClusterSize() <= Store.REPLICATION_FACTOR) {
            keysCopy.forEach(key -> this.store.copyFile(key, nodeID));
            return;
        }

        for (String key : keysCopy) {
            var closestEntry = this.store.getClosestMembershipInfo(key);
            var replications = this.store.getReplicationEntries(closestEntry);
            replications.put(closestEntry.getKey(), closestEntry.getValue());

            if (!replications.containsKey(nodeID) || replications.containsKey(this.store.getId()))
                continue;

            var next = closestEntry;
            for (int i = 0; i < Store.REPLICATION_FACTOR; i++) {
                next = this.store.getNextClosestMembershipInfo(next.getKey());
            }

            if (next.getKey().equals(this.store.getId())) this.store.transferFile(key);
        }
    }

}
