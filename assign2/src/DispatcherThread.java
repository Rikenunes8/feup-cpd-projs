import messages.Message;
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
            var from = header.getOrDefault("From", null);
            var key = header.getOrDefault("Key", null);
            if (type != null) {
                switch (type) {
                    case "JOIN" -> this.store.join();
                    case "LEAVE" -> this.store.leave();
                    case "PUT" -> {
                        TcpMessager.sendMessage(this.socket, Message.simpleMessage("ACK", ""));
                        if (!canProcess() || from == null || key == null) return;
                        var closestNode = this.store.getClosestMembershipInfo(key);
                        boolean coordinator = closestNode.getKey().equals(this.store.getId());
                        if (from.equals("client") && !coordinator) {
                            this.store.redirectService(closestNode.getValue(), this.message.getMessage());
                        } else {
                            if (from.equals("client") && coordinator) this.store.addCoordinator(key);
                            this.store.put(key, this.message.getBody());
                        }
                    }
                    case "GET" -> {
                        if (!canProcess()) return;
                        String response = this.store.get(header.get("Key"));
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "DELETE" -> {
                        TcpMessager.sendMessage(this.socket, Message.simpleMessage("ACK", ""));
                        if (!canProcess() || from == null || key == null) return;
                        var closestNode = this.store.getClosestMembershipInfo(key);
                        boolean coordinator = closestNode.getKey().equals(this.store.getId());
                        if (from.equals("client") && !coordinator) {
                            this.store.redirectService(closestNode.getValue(), this.message.getMessage());
                        } else {
                            if (from.equals("client") && coordinator) this.store.addCoordinator(key);
                            this.store.delete(key);
                        }
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
