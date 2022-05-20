import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class DispatcherThread implements Runnable{
    private final Socket socket;
    private final Store store;

    public DispatcherThread(Socket socket, Store store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try {
            String msg = TcpMessager.receiveMessage(socket);
            MessageBuilder message = new MessageBuilder(msg);

            Map<String, String> header = message.getHeader();
            if (header.containsKey("Type")) {
                switch (header.get("Type")) {
                    case "JOIN" -> store.join();
                    case "LEAVE" -> store.leave();
                    default -> System.out.println("Type not implemented");
                }
            } else if (header.containsKey("Operation")) {
                switch (header.get("Operation")) {
                    case "PUT" -> {
                        String response = store.put(header.get("Key"), message.getBody());
                        TcpMessager.sendMessage(socket, response);
                    }
                    case "GET" -> {
                        String response = store.get(header.get("Key"));
                        TcpMessager.sendMessage(socket, response);
                    }
                    case "DELETE" -> store.delete(header.get("Key"));
                    default -> System.out.println("Operation not implemented");
                }
            } else {
                System.out.println("Invalid Message!");
            }
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
