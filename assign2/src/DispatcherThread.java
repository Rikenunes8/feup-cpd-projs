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
            String response = MessageStore.ackMessage("FAILURE - Not online");
            Map<String, String> header = this.message.getHeader();
            var type = header.getOrDefault("Type", null);
            var key = header.getOrDefault("Key", null);
            if (type != null) {
                switch (type) {
                    case "JOIN" -> this.store.join();
                    case "LEAVE" -> this.store.leave();
                    case "PUT" -> {
                        if (!canProcess()) return;
                        response = this.store.put(key, this.message.getBody());
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "GET" -> {
                        if (!canProcess()) return;
                        response = this.store.get(key);
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "DELETE" -> {
                        if (!canProcess()) return;
                        response = this.store.delete(key);
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "REPLICA_PUT" -> {
                        if (!canProcess()) return;
                        response = this.store.replicaPut(key, this.message.getBody());
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "REPLICA_DEL" -> {
                        if (!canProcess()) return;
                        response = this.store.replicaDel(key);
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "REPLICA_GET" -> {
                        if (!canProcess()) return;
                        response = this.store.replicaGet(key);
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "REQUESTS" -> {
                        TcpMessager.sendMessage(this.socket, MessageStore.ackMessage("SUCCESS"));
                        if (this.store.isOnline()) this.store.mergePendingRequests(message);
                    }
                    default -> System.out.println("Type not implemented");
                }
            } else {
                System.out.println("Invalid Message!");
            }
            this.socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
