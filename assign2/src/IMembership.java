import java.rmi.Remote;

public interface IMembership extends Remote {
    boolean join();
    boolean leave();
}
