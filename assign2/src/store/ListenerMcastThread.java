package store;

import messages.MulticastMessager;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class ListenerMcastThread implements Runnable {
    private final Store store;

    public ListenerMcastThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        while (this.store.getMembershipCounter() % 2 == 0) {
            try {
                this.store.getRcvDatagramSocket().setSoTimeout(2*Store.ALARM_PERIOD);
                String msg = MulticastMessager.receiveMcastMessage(this.store.getRcvDatagramSocket());
                this.store.execute(new DispatcherMcastThread(this.store, msg));
            } catch (SocketTimeoutException e) {
                this.store.selectAlarmer(true, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
