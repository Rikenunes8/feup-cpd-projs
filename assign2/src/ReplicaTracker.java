import java.util.Set;
import java.util.TreeSet;

public class ReplicaTracker {
    private boolean active;
    private Set<String> replicas;
    public ReplicaTracker() {
        this.active = false;
        this.replicas = new TreeSet<>();
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    public void setReplicas(Set<String> replicas) {
        this.replicas = replicas;
    }

    public boolean isActive() {
        return active;
    }
    public Set<String> getReplicas() {
        return replicas;
    }

    public void addReplica(String key) {
        this.replicas.add(key);
    }
}
