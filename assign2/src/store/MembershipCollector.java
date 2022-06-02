package store;

import membership.MembershipView;
import messages.MessageStore;
import messages.TcpMessager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static messages.MulticastMessager.sendMcastMessage;

public class MembershipCollector {
    private static final int TIMEOUT = 1000;
    private static final int MAX_JOINS = 2; // TODO change to 3
    public static final int MAX_MS_MSG = 2; // TODO change to 3

    public static void collect(ServerSocket serverSocket, Store store) {
        final Map<String, MembershipView> membershipViews = new HashMap<>();
        System.out.println("Listening to Membership messages on port " + serverSocket.getLocalPort());

        // Notice cluster members of my join
        String msg = MessageStore.joinMessage(store.getId(), store.getNodeIP(), store.getStorePort(), store.getMembershipCounter(), serverSocket.getLocalPort());

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

        if (!membershipViews.isEmpty())
            store.getMembershipView().getMembershipLog().removeLastRecord();
        store.mergeMembershipViews(membershipViews);
        // store.updateMembershipView(store.getId(), store.getNodeIP(), store.getStorePort(), store.getMembershipCounter()); // Add itself to view

        System.out.println("Membership views synchronized"); // TODO DEBUG
    }

    public static void collectLight(ServerSocket serverSocket, Store store, boolean allLogs) {
        final Map<String, MembershipView> membershipViews = new HashMap<>();
        System.out.println("Listening to Membership messages on port " + serverSocket.getLocalPort());

        // Notice cluster members of my join

        String msg = MessageStore.msUpdateMessage(store.getId(), store.getNodeIP(), store.getStorePort(), store.getMembershipCounter(), serverSocket.getLocalPort(), allLogs);
        try {sendMcastMessage(msg, store.getSndDatagramSocket(), store.getMcastAddr(), store.getMcastPort());}
        catch (IOException ex) {throw new RuntimeException(ex);}
        do {
            var entry = membershipReaderTask(serverSocket);
            if (entry != null) membershipViews.put(entry.getKey(), entry.getValue());
            else { break; }
        } while (membershipViews.size() < MAX_MS_MSG);

        try {serverSocket.close();}
        catch (IOException e) {throw new RuntimeException(e);}

        System.out.println("+ MembershipViews size: " + membershipViews.size()); // TODO DEBUG

        store.mergeMembershipViews(membershipViews);
        store.updateMembershipView(store.getId(), store.getNodeIP(), store.getStorePort(), store.getMembershipCounter()); // Add itself to view

        System.out.println("Membership views synchronized"); // TODO DEBUG
    }

    private static Map.Entry<String, MembershipView> membershipReaderTask(ServerSocket serverSocket) {
        try {
            serverSocket.setSoTimeout(TIMEOUT);
            Socket socket = serverSocket.accept();

            System.out.println("New membership connection");
            String msg = TcpMessager.receiveMessage(socket);

            MessageStore message = new MessageStore(msg);
            String id = message.getHeader().get("NodeID");
            MembershipView membershipView = MessageStore.parseMembershipMessage(message);

            socket.close();
            return new AbstractMap.SimpleEntry<>(id, membershipView);
        }
        catch (SocketTimeoutException e) { System.out.println("Timeout occurred waiting for membership messages\n"); }
        catch (SocketException e) { System.out.println(e.getMessage()); }
        catch (IOException e) { throw new RuntimeException(); }

        return null;
    }
}
