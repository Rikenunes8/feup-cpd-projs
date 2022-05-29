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
            if (header.containsKey("Type")) {
                switch (header.get("Type")) {
                    case "JOIN" -> this.store.join();
                    case "LEAVE" -> this.store.leave();
                    case "PUT" -> {
                        if (!canProcess()) return;
                        this.store.put(header.get("Key"), this.message.getBody());
                    }
                    case "GET" -> {
                        if (!canProcess()) return;
                        String response = this.store.get(header.get("Key"));
                        TcpMessager.sendMessage(this.socket, response);
                    }
                    case "DELETE" -> {
                        if (!canProcess()) return;
                        this.store.delete(header.get("Key"));
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
