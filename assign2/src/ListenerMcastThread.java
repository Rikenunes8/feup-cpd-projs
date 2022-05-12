
import messages.MessageBuilder;

import java.io.IOException;
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
        ExecutorService executor = Executors.newFixedThreadPool(4);
        while (this.store.getMembershipCounter() % 2 == 0) {
            try {
                String msg = receiveMcastMessage(store.getRcvDatagramSocket());
                MessageBuilder message = new MessageBuilder(msg);
                switch (message.getHeader().get("Type")) {
                    case "JOIN" -> System.out.println("Received join message from " + message.getHeader().get("NodeIP"));
                    case "LEAVE" -> System.out.println("Received leave message from " + message.getHeader().get("NodeIP"));
                    default -> System.out.println("Type case not implemented");
                }

                // System.out.println(msg); // TODO
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
