import membership.MembershipView;
import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class MembershipReaderTask implements Callable<Map.Entry<String, MembershipView>> {
    private final ServerSocket serverSocket;

    public MembershipReaderTask(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public Map.Entry<String, MembershipView> call() {
        try {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("New membership connection");
            String msg = TcpMessager.receiveMessage(socket);

            System.out.println(msg); // TODO DEBUG
            MessageBuilder message = new MessageBuilder(msg);
            String id = message.getHeader().get("NodeIP");
            MembershipView membershipView = MessageBuilder.parseMembershipMessage(message);

            socket.close();
            return new AbstractMap.SimpleEntry<>(id, membershipView);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
