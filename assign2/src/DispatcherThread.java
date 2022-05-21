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
                    case "PUT" -> {
                        // TODO beta
                        int c = 0;
                        while (!this.store.isOnline() && c < 3) {
                            System.out.println("Not online yet"); Thread.sleep(1000); c++;
                        }
                        // TODO -------------
                        String key = header.get("Key");
                        String response = store.put(key, message.getBody());
                        if (key == null || key.equals("null") || key.isEmpty())
                            TcpMessager.sendMessage(socket, response);
                    }
                    case "GET" -> {
                        String response = store.get(header.get("Key"));
                        TcpMessager.sendMessage(socket, response);
                    }
                    case "DELETE" -> store.delete(header.get("Key"));
                    default -> System.out.println("Type not implemented");
                }
            } else {
                System.out.println("Invalid Message!");
            }
            this.socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
