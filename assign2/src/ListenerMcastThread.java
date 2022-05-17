
import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                String msg = receiveMcastMessage(store.getRcvDatagramSocket());
                MessageBuilder message = new MessageBuilder(msg);
                
                Map<String, String> header = message.getHeader();
                switch (header.get("Type")) {
                    case "JOIN" -> joinHandler(header);
                    case "LEAVE" -> leaveHandler(header);
                    default -> System.out.println("Type case not implemented");
                }
                System.out.println("---- MS VIEW AFTER LISTENING MCAST----");
                System.out.println("MS Log:\n" + store.getMembershipLog());
                System.out.println("MS Tab:\n" + store.getMembershipTable());
                System.out.println("---- END MS VIEW ----");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void joinHandler(Map<String, String> header) {
        System.out.println("Received join message from " + header.get("NodeIP"));
        String nodeIP = header.get("NodeIP");
        int msPort    = Integer.parseInt(header.get("MembershipPort"));
        int nodePort  = Integer.parseInt(header.get("Port"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));

        this.store.addJoinLeaveEvent(nodeIP, nodePort, msCounter);

        if (!this.store.getNodeIP().equals(nodeIP)) {
            String msMsg = MessageBuilder.membershipMessage(this.store.getMembershipLog(), this.store.getMembershipTable(), this.store.getNodeIP());
            try { TcpMessager.sendMessage(nodeIP, msPort, msMsg); } // TODO Should not resend if no changes since last time
            catch (IOException e) { System.out.println("ERROR: " + e.getMessage()); }
        }
    }

    private void leaveHandler(Map<String, String> header) {
        System.out.println("Received leave message from " + header.get("NodeIP"));
        String nodeIP = header.get("NodeIP");
        int nodePort  = Integer.parseInt(header.get("Port"));
        int msCounter = Integer.parseInt(header.get("MembershipCounter"));
        this.store.addJoinLeaveEvent(nodeIP, nodePort, msCounter);
    }

}
