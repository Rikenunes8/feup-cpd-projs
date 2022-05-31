package messages;

import java.io.*;
import java.net.Socket;

import java.util.AbstractMap;
import java.util.Map;

public class TcpMessager {
    public static String sendAndReceiveMessage(String ip, int port, String message) throws IOException {
        try (Socket socket = new Socket(ip, port)) {
            sendMessage(socket, message);
            return receiveMessage(socket);
        }
    }

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
        StringBuilder stringBuilder = new StringBuilder();
        InputStream input = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        int bodySize = 0;
        while (true) {
            String line = reader.readLine();
            stringBuilder.append(line).append("\n");
            var entry = parseHeaderEntry(line);
            if (entry == null) break;
            if (entry.getKey().equals("BodySize")) bodySize = Integer.parseInt(entry.getValue());
        }
        for (int i = 0; i < bodySize; i++) {
            int c = reader.read();
            if (c == -1) break;
            stringBuilder.append(Character.toString(c));
        }
        return stringBuilder.toString();
    }

    private static Map.Entry<String, String> parseHeaderEntry(String line) {
        String[] keyVal = line.split(": ");
        if (keyVal.length <= 1) return null;
        return new AbstractMap.SimpleEntry<>(keyVal[0], keyVal[1]);
    }
}
