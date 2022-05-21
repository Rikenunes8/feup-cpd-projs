import membership.MembershipTable;
import membership.MembershipView;
import messages.MessageBuilder;
import messages.TcpMessager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.sendMcastMessage;

public class MembershipCollector {
    private static final int TIMEOUT = 1000;
    private static final int MAX_JOINS = 2;
    private static final int MAX_MS_MSG = 2;

    public static void collect(ServerSocket serverSocket, Store store) {
        final ConcurrentHashMap<String, MembershipView> membershipViews = new ConcurrentHashMap<>();
        System.out.println("Listening for Membership messages on port " + serverSocket.getLocalPort());

        // Notice cluster members of my join
        String msg = messageJoinLeave(store.getNodeIP(), store.getStorePort(), store.getMembershipCounter(), serverSocket.getLocalPort());

        boolean send = true;
        int attempts = 0;
        do {
            if (send) {
                try { sendMcastMessage(msg, store.getSndDatagramSocket(), store.getMcastAddr(), store.getMcastPort());}
                catch (IOException e) {
                    try {serverSocket.close();}
                    catch (IOException ex) {throw new RuntimeException(ex);}
                    throw new RuntimeException(e);
                }
                System.out.println("Join message sent!");
                send = false;
            }

            var entry = membershipReaderTask(serverSocket);
            if (entry != null) membershipViews.put(entry.getKey(), entry.getValue());
            else { attempts++; send = true; }
        } while (membershipViews.size() < MAX_MS_MSG && attempts < MAX_JOINS);

        try {serverSocket.close();}
        catch (IOException e) {throw new RuntimeException(e);}

        System.out.println("+ MembershipViews size: " + membershipViews.size()); // TODO DEBUG
        System.out.println(store.getMembershipView().getMembershipTable());
        System.out.println(store.getMembershipView().getMembershipLog());

        store.updateMembershipView(store.getNodeIP(), store.getStorePort(), store.getMembershipCounter()); // Add itself to view
        store.mergeMembershipViews(membershipViews);

        System.out.println("Membership views synchronized"); // TODO DEBUG
        System.out.println(store.getMembershipView().getMembershipTable());
        System.out.println(store.getMembershipView().getMembershipLog());
    }

    private static Map.Entry<String, MembershipView> membershipReaderTask(ServerSocket serverSocket) {
        try {
            serverSocket.setSoTimeout(TIMEOUT);
            Socket socket = serverSocket.accept();

            System.out.println("New membership connection");
            String msg = TcpMessager.receiveMessage(socket);

            MessageBuilder message = new MessageBuilder(msg);
            String id = message.getHeader().get("NodeIP");
            MembershipView membershipView = MessageBuilder.parseMembershipMessage(message);

            socket.close();
            return new AbstractMap.SimpleEntry<>(id, membershipView);
        }
        catch (SocketTimeoutException e) { System.out.println("Timeout?!"); }
        catch (SocketException e) { System.out.println(e.getMessage()); }
        catch (IOException e) { throw new RuntimeException(); }

        return null;
    }
}
