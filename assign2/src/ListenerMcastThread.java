
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
                    case "JOIN" -> {
                        System.out.println("Received join message from " + header.get("NodeIP"));
                        String nodeIP = header.get("NodeIP");
                        int nodePort = Integer.parseInt(header.get("Port"));
                        this.store.addJoinLeaveEvent(
                                nodeIP,
                                nodePort,
                                Integer.parseInt(header.get("MembershipCounter"))
                        );
                        String msMsg = MessageBuilder.membershipMessage(this.store.getMembershipLog(), this.store.getMembershipTable(), this.store.getNodeIP());
                        TcpMessager.sendMessage(nodeIP, nodePort, msMsg); // TODO Should not resend if no changes since last time
                    }
                    case "LEAVE" -> {
                        System.out.println("Received leave message from " + header.get("NodeIP"));
                        this.store.addJoinLeaveEvent(
                                header.get("NodeIP"),
                                Integer.parseInt(header.get("Port")),
                                Integer.parseInt(header.get("MembershipCounter"))
                        );
                    }
                    default -> System.out.println("Type case not implemented");
                }

                System.out.println("MS Log:\n" + store.getMembershipLog());
                System.out.println("MS Tab:\n" + store.getMembershipTable());

                // System.out.println(msg); // DEBUG
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
