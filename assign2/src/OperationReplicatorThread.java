public class OperationReplicatorThread implements Runnable  {
    private static final int MAX_RETRANSMISSIONS = 3;
    private final Store store;
    private final String replicaID;
    private final String message;

    public OperationReplicatorThread(Store store, String replicaID, String message) {
        this.store = store;
        this.replicaID = replicaID;
        this.message = message;
    }
    @Override
    public void run() {
        String resp;
        int c = 0;
        do {
            resp = this.store.redirect(this.store.getMembershipInfo(replicaID), this.message);
            if (resp == null) {
                c++;
                try { Thread.sleep(1000);}
                catch (InterruptedException e) { System.out.println(e.getMessage());}
            }
        } while (resp == null && c < MAX_RETRANSMISSIONS);
        if (c == MAX_RETRANSMISSIONS) {
            // TODO save info to try again latter
        }
    }
}

