import messages.Message;
import messages.MessageStore;
import messages.TcpMessager;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class DispatcherThread implements Runnable {
    private final Socket socket;
    private final Store store;
    private Message message;

    public DispatcherThread(Socket socket, Store store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try {
            String msg = TcpMessager.receiveMessage(this.socket);
            this.message = new Message(msg);
            this.processMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processMessage() {
        try {
            Map<String, String> header = this.message.getHeader();
            var type = header.getOrDefault("Type", null);
            var key = header.getOrDefault("Key", null);
            if (type != null) {
                switch (type) {
                    case "JOIN" -> this.store.join();
                    case "LEAVE" -> this.store.leave();
                    case "PUT" -> {
                        if (!canProcess() || key == null) return;
                        String response = this.store.put(key, this.message.getBody());
                        TcpMessager.sendMessage(this.socket, Message.ackMessage(new MessageStore(response).getBody()));
                    }
                    case "GET" -> {
                        if (!canProcess() || key == null) return;
                        String response = this.store.get(key);
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "DELETE" -> {
                        if (!canProcess() || key == null) return;
                        String response = this.store.delete(key);
                        TcpMessager.sendMessage(this.socket, Message.ackMessage(new MessageStore(response).getBody()));
                    }
                    case "REPLICA" -> {
                        if (!canProcess() || key == null) return;
                        String response = this.store.replica(key, this.message.getBody());
                        TcpMessager.sendMessage(this.socket, Message.ackMessage(new MessageStore(response).getBody()));
                    }
                    case "DEL_REPLICA" -> {
                        if (!canProcess() || key == null) return;
                        String response = this.store.delReplica(key);
                        TcpMessager.sendMessage(this.socket, Message.ackMessage(new MessageStore(response).getBody()));
                    }

                    default -> System.out.println("Type not implemented");
                }
            } else {
                System.out.println("Invalid Message!");
            }
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean canProcess() {
        if (!store.isOnline()) {
            store.addToPendingQueue(this);
            return false;
        }
        return true;
    }
}
