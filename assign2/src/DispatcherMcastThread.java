
import java.io.IOException;

import static messages.MulticastMessager.receiveMcastMessage;

public class DispatcherMcastThread implements Runnable {
    private final Store store;

    public DispatcherMcastThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        while (this.store.getMembershipCounter() % 2 == 0) {
            try {
                String msg = receiveMcastMessage(store.getRcvDatagramSocket());
                System.out.println(msg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
