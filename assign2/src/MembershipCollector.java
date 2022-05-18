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
            System.out.println("+ WMembershipViews size: " + membershipViews.size()); // TODO DEBUG

            var entry = membershipReaderTask(serverSocket);
            if (entry != null) membershipViews.put(entry.getKey(), entry.getValue());
            else { attempts++; send = true; }
        } while (membershipViews.size() < MAX_MS_MSG && attempts < MAX_JOINS);

        try {serverSocket.close();}
        catch (IOException e) {throw new RuntimeException(e);}

        System.out.println("+ MembershipViews size: " + membershipViews.size()); // TODO DEBUG

        MembershipView membershipView = mergeMembershipViews(membershipViews, store);
        store.setMembershipTable(membershipView.getMembershipTable());
        store.setMembershipLog(membershipView.getMembershipLog());
        System.out.println("Membership views synchronized"); // TODO DEBUG
    }

    private static Map.Entry<String, MembershipView> membershipReaderTask(ServerSocket serverSocket) {
        try {
            System.out.println("Waiting for accepting membershipMessages [MRT]");
            serverSocket.setSoTimeout(TIMEOUT);
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
        }
        catch (SocketTimeoutException e) { System.out.println("Timeout?!"); }
        catch (SocketException e) { System.out.println(e.getMessage()); }
        catch (IOException e) { throw new RuntimeException(); }

        return null;
    }

    private static MembershipView mergeMembershipViews(Map<String, MembershipView> membershipViews, Store store) {
        MembershipView merged = new MembershipView(store.getMembershipTable(), store.getMembershipLog()); // Store view
        for (var pair : membershipViews.entrySet()) {
            merged.getMembershipLog().mergeLogs(pair.getValue().getMembershipLog().last32Logs());
            MembershipTable membershipTable = merged.getMembershipTable(); // TODO merge table
            merged = new MembershipView(membershipTable, merged.getMembershipLog());
        }
        return merged;
    }
}
