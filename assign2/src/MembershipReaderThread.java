import java.io.*;
import java.net.Socket;

public class MembershipReaderThread extends Thread {
    private final Socket socket;
    private String message;

    public MembershipReaderThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String msg = reader.readLine();
            this.message = msg;
            System.out.println(msg);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMessage() {
        return this.message;
    }
}
