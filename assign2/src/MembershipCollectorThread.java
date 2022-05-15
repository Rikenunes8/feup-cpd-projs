import membership.MembershipLog;
import membership.MembershipTable;
import membership.MembershipView;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.*;

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.sendMcastMessage;

public class MembershipCollectorThread implements Runnable {
    private final ConcurrentHashMap<String, MembershipView> membershipViews;
    private final ServerSocket serverSocket;
    private final Store store;

    public MembershipCollectorThread(ServerSocket serverSocket, Store store) {
        this.serverSocket = serverSocket;
        this.store = store;

        this.membershipViews = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        int maxMembershipMessages = 2;
        int maxJoinResends = 3;
        System.out.println("Listening on port " + serverSocket.getLocalPort());

        // Notice cluster members of my join
        String msg = messageJoinLeave(this.store.getNodeIP(), this.store.getStorePort(), this.store.getMembershipCounter(), this.store.getMembershipPort());
        try {
            sendMcastMessage(msg, this.store.getSndDatagramSocket(), this.store.getMcastAddr(), this.store.getMcastPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Join message sent!");


        int attempts = 0;
        while (this.membershipViews.size() < maxMembershipMessages && attempts < maxJoinResends) {
            System.out.println("WMembershipViews size: " + this.membershipViews.size()); // TODO DEBUG

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            Future<Map.Entry<String, MembershipView>> future = executor.submit(new MembershipReaderTask(serverSocket));
            Runnable cancelTask = () -> future.cancel(true);

            executor.schedule(cancelTask, 3, TimeUnit.SECONDS);
            executor.shutdown();

            try {
                System.out.println("Waiting fot get"); // TODO
                Map.Entry<String, MembershipView> entry = future.get();
                System.out.println("Get received"); // TODO
                membershipViews.put(entry.getKey(), entry.getValue());
            } catch (CancellationException e) {
                System.out.println("Get failed"); // TODO
                attempts++;

                try {
                    sendMcastMessage(msg, this.store.getSndDatagramSocket(), this.store.getMcastAddr(), this.store.getMcastPort());
                    System.out.println("Join message resent!");
                } catch (IOException ignored) {
                    System.out.println(ignored);
                }
            } catch (ExecutionException | InterruptedException e) {
                System.out.println("Fuck"); // TODO
                System.out.println(e);
            }
        }
        System.out.println("MembershipViews size: " + this.membershipViews.size()); // TODO DEBUG

        MembershipView membershipView = this.mergeMembershipView();
        this.store.setMembershipTable(membershipView.getMembershipTable());
        this.store.setMembershipLog(membershipView.getMembershipLog());
    }

    private MembershipView mergeMembershipView() {
        MembershipView merged = new MembershipView(new MembershipTable(), new MembershipLog());
        for (var pair : this.membershipViews.entrySet()) {
            merged = mergeTwoMembershipViews(merged, pair.getValue());
        }

        merged = mergeTwoMembershipViews(merged, new MembershipView(this.store.getMembershipTable(), this.store.getMembershipLog()));

        return merged;
    }

    // TODO actual merge something
    private MembershipView mergeTwoMembershipViews(MembershipView merged, MembershipView value) {
        MembershipLog membershipLog = merged.getMembershipLog();
        MembershipTable membershipTable = merged.getMembershipTable();
        return new MembershipView(membershipTable, membershipLog);
    }
}
