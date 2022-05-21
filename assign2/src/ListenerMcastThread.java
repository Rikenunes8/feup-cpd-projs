
import membership.MembershipView;
import messages.MessageBuilder;
import messages.TcpMessager;
import utils.HashUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import static messages.MessageBuilder.parseMembershipMessage;
import static messages.MulticastMessager.receiveMcastMessage;

public class ListenerMcastThread implements Runnable {
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
        String nodeIP = header.get("NodeIP");
        int msPort    = Integer.parseInt(header.get("MembershipPort"));
        int nodePort  = Integer.parseInt(header.get("Port"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));

        this.store.updateMembershipView(nodeIP, nodePort, msCounter);

        if (this.store.getNodeIP().equals(nodeIP)) return;

        Thread.sleep(new Random().nextInt(500)); // Wait random time before send membership message

        String msMsg = MessageBuilder.membershipMessage(this.store.getMembershipView(), this.store.getNodeIP());
        try { TcpMessager.sendMessage(nodeIP, msPort, msMsg); } // TODO Should not resend if no changes since last time
        catch (IOException e) { System.out.println("ERROR: " + e.getMessage()); }

        var keysCopy = new HashSet<>(this.store.getKeys());
        for (String key : keysCopy) {
            if (this.store.getClosestMembershipInfo(key).toString().equals(HashUtils.joinIpPort(nodeIP, nodePort))) {
                this.store.transferFile(key);
            }
        }
    }

    private void leaveHandler(Map<String, String> header) {
        System.out.println("Received leave message from " + header.get("NodeIP"));
        String nodeIP = header.get("NodeIP");
        int nodePort  = Integer.parseInt(header.get("Port"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));
        this.store.updateMembershipView(nodeIP, nodePort, msCounter);
    }

    private void membershipHandler(MessageBuilder message) {
        System.out.println("Multicast Membership Message received");
        MembershipView view = parseMembershipMessage(message);
        this.store.updateMembershipView(view.getMembershipTable(), view.getMembershipLog());
    }
}
