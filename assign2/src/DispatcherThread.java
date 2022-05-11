import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class DispatcherThread implements Runnable{
    private final Store store;

    public DispatcherThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.store.getPort())){
            Socket socket = serverSocket.accept();

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String msg = reader.readLine();
            switch (msg) {
                case "join" -> store.join();
                case "leave" -> store.leave();
            }
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
