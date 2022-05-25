import messages.MessageBuilder;

import java.io.IOException;

import static messages.MulticastMessager.sendMcastMessage;

public class AlarmThread implements Runnable{
    private final Store store;
    public AlarmThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        System.out.println("Sending alarm");
        String msg = MessageBuilder.membershipMessage(this.store.getNodeIP(), this.store.getMembershipView());
        try {
            sendMcastMessage(msg, store.getSndDatagramSocket(), store.getMcastAddr(), store.getMcastPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
