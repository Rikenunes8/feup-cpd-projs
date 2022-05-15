package messages;

import java.io.*;
import java.net.Socket;

public class TcpMessager {
    public static void sendMessage(String ip, int port, String message) {
        try (Socket socket = new Socket(ip, port)) {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String receiveMessage(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        return reader.readLine();
    }
}
