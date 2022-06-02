package store;

public class OperationReplicatorThread implements Runnable  {
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
        String resp = this.store.redirect(this.store.getMembershipInfo(this.replicaID), this.message);
        if (resp == null) {
            this.store.addPendingRequest(this.replicaID, this.message);
        }
    }
}

