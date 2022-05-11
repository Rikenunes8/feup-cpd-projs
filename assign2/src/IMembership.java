import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IMembership extends Remote {
    boolean join() throws RemoteException;
    boolean leave() throws RemoteException;
}
