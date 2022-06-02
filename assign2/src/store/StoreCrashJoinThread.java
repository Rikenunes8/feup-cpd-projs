package store;

public class StoreCrashJoinThread implements Runnable {
    private final Store store;

    public StoreCrashJoinThread(Store store) {
        this.store = store;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            this.store.rejoin();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
