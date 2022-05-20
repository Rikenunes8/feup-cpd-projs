package messages;

import java.io.*;
import java.net.Socket;
import java.util.stream.Collectors;

import static messages.MessageBuilder.CR;
import static messages.MessageBuilder.LF;

public class TcpMessager {
    public static void sendMessage(String ip, int port, String message) throws IOException {
        try (Socket socket = new Socket(ip, port)) {
            sendMessage(socket, message);
        }
    }
    public static void sendMessage(Socket socket, String message) throws IOException {
        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);
        writer.println(message);
    }
    public static String receiveMessage(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        return reader.lines().collect(Collectors.joining(Character.toString(CR) + LF));
    }
}
