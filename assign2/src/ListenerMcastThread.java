
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
                System.out.println(msg); // TODO
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
