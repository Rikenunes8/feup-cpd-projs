import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import static messages.MulticastMessager.sendMcastMessage;

public class AlarmThread implements Runnable{
    private final Store store;
    private final String msg;

    public AlarmThread(Store store, String msg) {
        this.store = store;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            sendMcastMessage(this.msg, store.getSndDatagramSocket(), store.getMcastAddr(), store.getMcastPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
