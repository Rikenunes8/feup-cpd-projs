import membership.MembershipLog;
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

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.sendMcastMessage;

public class MembershipCollectorThread implements Runnable {
    private final ConcurrentHashMap<String, MembershipView> membershipViews;
    private final ServerSocket serverSocket;
    private final Store store;

    public MembershipCollectorThread(ServerSocket serverSocket, Store store) {
        this.store = store;
        this.serverSocket = serverSocket;
        this.membershipViews = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        int maxJoinResends = 2;
        int maxMembershipMessages = 2;
        System.out.println("Listening for Membership messages on port " + serverSocket.getLocalPort());

        // Notice cluster members of my join
        String msg = messageJoinLeave(this.store.getNodeIP(), this.store.getStorePort(), this.store.getMembershipCounter(), serverSocket.getLocalPort());

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
            if (entry != null) membershipViews.put(entry.getKey(), entry.getValue());
            else { attempts++; send = true; }
        } while (this.membershipViews.size() < maxMembershipMessages && attempts < maxJoinResends);

        try {this.serverSocket.close();}
        catch (IOException e) {throw new RuntimeException(e);}

        System.out.println("+ MembershipViews size: " + this.membershipViews.size()); // TODO DEBUG

        MembershipView membershipView = this.mergeMembershipView();
        this.store.setMembershipTable(membershipView.getMembershipTable());
        this.store.setMembershipLog(membershipView.getMembershipLog());
        System.out.println("Membership views synchronized"); // TODO DEBUG
    }

    public Map.Entry<String, MembershipView> membershipReaderTask() {
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
        }
        catch (SocketTimeoutException e) { System.out.println("Timeout?!"); }
        catch (SocketException e) { System.out.println(e.getMessage()); }
        catch (IOException e) { throw new RuntimeException(); }

        return null;
    }

    private MembershipView mergeMembershipView() {
        MembershipView merged = new MembershipView(new MembershipTable(), new MembershipLog());
        for (var pair : this.membershipViews.entrySet()) {
            merged = mergeTwoMembershipViews(merged, pair.getValue());
        }

        return mergeTwoMembershipViews(merged, new MembershipView(this.store.getMembershipTable(), this.store.getMembershipLog()));
    }

    // TODO actual merge something
    private MembershipView mergeTwoMembershipViews(MembershipView merged, MembershipView value) {
        MembershipLog membershipLog = merged.getMembershipLog();
        MembershipTable membershipTable = merged.getMembershipTable();
        return new MembershipView(membershipTable, membershipLog);
    }
}
