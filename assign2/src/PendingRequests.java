import java.util.*;

public class PendingRequests {
    private final Map<String, List<String>> pendingMessages;


    public PendingRequests() {
        this.pendingMessages = new HashMap<>();
    }

    public void addRequest(String nodeID, String message) {
        if (this.pendingMessages.containsKey(nodeID)) {
            this.pendingMessages.get(nodeID).add(message);
        }
        else {
            List<String> messages = new ArrayList<>();
            messages.add(message);
            this.pendingMessages.put(nodeID, messages);
        }
    }

    public boolean hasPendingRequests(String nodeID) {
        return this.pendingMessages.containsKey(nodeID) && !this.pendingMessages.get(nodeID).isEmpty();
    }

    public void empty(String nodeID) {
        this.pendingMessages.remove(nodeID);
    }

    public List<String> getNodePendingRequests(String nodeID) {
        if (this.pendingMessages.containsKey(nodeID)) {
            List<String> requests = this.pendingMessages.get(nodeID);
            this.pendingMessages.remove(nodeID);
            return requests;
        }
        return new ArrayList<>();
    }

    public Map<String, List<String>> getPendingMessages() {
        return pendingMessages;
    }

    public void clear() {
        this.pendingMessages.clear();
    }
}
