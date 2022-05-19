import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DispatcherRMIThread implements Runnable {

    private Store store;

    public DispatcherRMIThread(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Registry registry = LocateRegistry.createRegistry(store.getStorePort());
                registry.rebind("membership", store);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
