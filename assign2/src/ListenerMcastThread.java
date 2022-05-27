import messages.MulticastMessager;

import java.io.IOException;

public class ListenerMcastThread implements Runnable {
    private final Store store;

    public ListenerMcastThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        while (this.store.getMembershipCounter() % 2 == 0) {
            try {
                String msg = MulticastMessager.receiveMcastMessage(this.store.getRcvDatagramSocket());
                this.store.execute(new DispatcherMcastThread(this.store, msg));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
