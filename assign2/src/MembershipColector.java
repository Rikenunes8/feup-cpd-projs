import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MembershipColector extends Thread {
    private final String id;
    private final int port;
    private List<String> membershipViews;

    public MembershipColector(String id, int port) {
        this.id = id;
        this.port = port;
        this.membershipViews = new ArrayList<>();
    }

    @Override
    public void run() {
        List<MembershipReader> threads = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {

            System.out.println("Store (" + this.id +") is listening on port " + this.port);
            int i = 0;
            while (i < 1) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                MembershipReader reader = new MembershipReader(socket);
                threads.add(reader);
                reader.start();
                i++;
            }
            for (MembershipReader reader : threads) {
                reader.join();
                membershipViews.add(reader.getMessage());
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getMembershipViews() {
        return this.membershipViews;
    }
}
