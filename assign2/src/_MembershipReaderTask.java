import membership.MembershipView;
import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class _MembershipReaderTask implements Callable<Map.Entry<String, MembershipView>> {
    private final ServerSocket serverSocket;

    public _MembershipReaderTask(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public Map.Entry<String, MembershipView> call() {
        try {
            System.out.println("Waiting for accepting membershipMessages [MRT]");
            serverSocket.setSoTimeout(1000);
            Socket socket = serverSocket.accept();
            System.out.println("MembershipMessage accepted [MRT]");

            System.out.println("New membership connection");
            String msg = TcpMessager.receiveMessage(socket);
            System.out.println("--- Membership Message Received ---");
            System.out.println(msg); // TODO DEBUG
            System.out.println("--- END membership message ---");

            MessageBuilder message = new MessageBuilder(msg);
            String id = message.getHeader().get("NodeIP");
            MembershipView membershipView = MessageBuilder.parseMembershipMessage(message);

            socket.close();
            return new AbstractMap.SimpleEntry<>(id, membershipView);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("Timeout?!");
        }
        return null;
    }
}
