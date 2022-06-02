package store;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PendingRequests {
    private final Map<String, AbstractQueue<String>> pendingMessages;


    public PendingRequests() {
        this.pendingMessages = new HashMap<>();
    }

    public void addRequest(String nodeID, String message) {
        if (this.pendingMessages.containsKey(nodeID)) {
            this.pendingMessages.get(nodeID).add(message);
        }
        else {
            AbstractQueue<String> messages = new ConcurrentLinkedQueue<>();
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

    public AbstractQueue<String> getNodePendingRequests(String nodeID) {
        if (this.pendingMessages.containsKey(nodeID)) {
            AbstractQueue<String> requests = this.pendingMessages.get(nodeID);
            this.pendingMessages.remove(nodeID);
            return requests;
        }
        return new ConcurrentLinkedQueue<>();
    }

    public Map<String, AbstractQueue<String>> getPendingMessages() {
        return pendingMessages;
    }

    public void clear() {
        this.pendingMessages.clear();
    }
}
