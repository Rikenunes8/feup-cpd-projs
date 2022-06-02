package store;

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
            if (this.store.isOnline()) {
                Map<String, String> header = this.message.getHeader();
                var type = header.getOrDefault("Type", null);
                var key = header.getOrDefault("Key", null);
                if (type != null) {
                    System.out.println("Received " + type + " message");
                    switch (type) {
                        case "PUT" -> response = this.store.put(key, this.message.getBody());
                        case "GET" -> response = this.store.get(key);
                        case "DELETE" -> response = this.store.delete(key);
                        case "REPLICA_PUT" -> response = this.store.replicaPut(key, this.message.getBody());
                        case "REPLICA_DEL" -> response = this.store.replicaDel(key);
                        case "REPLICA_GET" -> response = this.store.replicaGet(key);
                        case "REQUESTS" -> {
                            response = MessageStore.ackMessage("SUCCESS");
                            TcpMessager.sendMessage(this.socket, response);
                            this.store.mergePendingRequests(message);
                            this.socket.close();
                            return;
                        }
                        default -> response = MessageStore.ackMessage("Type not implemented");
                    }
                } else {
                    response = MessageStore.ackMessage("Invalid Message!");
                }
            }
            TcpMessager.sendMessage(this.socket, response);
            this.socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
