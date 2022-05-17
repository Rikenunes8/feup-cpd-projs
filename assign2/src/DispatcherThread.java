import messages.TcpMessager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

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

            switch (msg) {
                case "join" -> store.join();
                case "leave" -> store.leave();
                case "show" -> { // TODO DEBUG
                    System.out.println("\n--- MEMBERSHIP VIEW ---");
                    System.out.println("Membership Table");
                    System.out.println(store.getMembershipTable());
                    System.out.println("Membership Logs");
                    System.out.println(store.getMembershipLog());
                    System.out.println("--- END MEMBERSHIP VIEW ---\n");
                }
                default -> System.out.println("Operation not implemented");
            }
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
