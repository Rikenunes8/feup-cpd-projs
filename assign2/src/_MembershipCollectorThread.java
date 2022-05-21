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
import java.util.concurrent.*;

import static messages.MessageBuilder.joinLeaveMessage;
import static messages.MulticastMessager.sendMcastMessage;

public class _MembershipCollectorThread implements Runnable {
    private static final int TIMEOUT = 1000;
    private static final int MAX_JOINS = 2;
    private static final int MAX_MS_MSG = 2;

    private final ConcurrentHashMap<String, MembershipView> membershipViews;
    private final ServerSocket serverSocket;
    private final Store store;

    public _MembershipCollectorThread(ServerSocket serverSocket, Store store) {
        this.store = store;
        this.serverSocket = serverSocket;
        this.membershipViews = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        System.out.println("Listening for Membership messages on port " + serverSocket.getLocalPort());

        // Notice cluster members of my join
        String msg = joinLeaveMessage(this.store.getNodeIP(), this.store.getStorePort(), this.store.getMembershipCounter(), serverSocket.getLocalPort());

        boolean send = true;
        int attempts = 0;
        do {
            if (send) {
                try { sendMcastMessage(msg, this.store.getSndDatagramSocket(), this.store.getMcastAddr(), this.store.getMcastPort());}
                catch (IOException e) {
                    try {this.serverSocket.close();}
                    catch (IOException ex) {throw new RuntimeException(ex);}
                    throw new RuntimeException(e);
                }
                System.out.println("Join message sent!");
                send = false;
            }
            System.out.println("+ WMembershipViews size: " + this.membershipViews.size()); // TODO DEBUG

            var entry = membershipReaderTask();
            if (entry != null) this.membershipViews.put(entry.getKey(), entry.getValue());
            else { attempts++; send = true; }
        } while (this.membershipViews.size() < MAX_MS_MSG && attempts < MAX_JOINS);

        try {this.serverSocket.close();}
        catch (IOException e) {throw new RuntimeException(e);}

        System.out.println("+ MembershipViews size: " + this.membershipViews.size()); // TODO DEBUG

        MembershipView membershipView = this.mergeMembershipViews();
        this.store.setMembershipView(membershipView.getMembershipTable(), membershipView.getMembershipLog());
        System.out.println("Membership views synchronized"); // TODO DEBUG
    }

    public Map.Entry<String, MembershipView> membershipReaderTask() {
        try {
            System.out.println("Waiting for accepting membershipMessages [MRT]");
            this.serverSocket.setSoTimeout(TIMEOUT);
            Socket socket = this.serverSocket.accept();
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

    private MembershipView mergeMembershipViews() {
        MembershipView merged = this.store.getMembershipView();
        for (var pair : this.membershipViews.entrySet()) {
            merged = mergeTwoMembershipViews(merged, pair.getValue());
        }
        return merged;
    }

    // TODO actual merge something
    private MembershipView mergeTwoMembershipViews(MembershipView merged, MembershipView value) {
        merged.getMembershipLog().mergeLogs(value.getMembershipLog().last32Logs());
        MembershipTable membershipTable = merged.getMembershipTable(); // TODO merge table
        return new MembershipView(membershipTable, merged.getMembershipLog());
    }
}
