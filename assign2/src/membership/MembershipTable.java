package membership;

import java.util.*;

public class MembershipTable {

    private final TreeMap<String, MembershipInfo> membershipInfoMap;

    public MembershipTable() {
        this.membershipInfoMap = new TreeMap<>();
    }

    public void addMembershipInfo(String id, MembershipInfo msInfo) {
        this.membershipInfoMap.put(id, msInfo);
    }

    public void removeMembershipInfo(String id) {
        this.membershipInfoMap.remove(id);
    }

    public TreeMap<String, MembershipInfo> getMembershipInfoMap() {
        return this.membershipInfoMap;
    }

    public Map.Entry<String, MembershipInfo> getClosestMembershipInfo(String key) {
        if (this.membershipInfoMap.isEmpty()) return null;
        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.ceilingEntry(key); // Binary Search to find the closest node

        // the key is after the last hashedId in the tree
        return (closestNode == null)
                // : reassign the closest value as the first one in the tree
                ? this.membershipInfoMap.firstEntry()
                : closestNode;
    }

    public Map.Entry<String, MembershipInfo> getNextClosestMembershipInfo(String nodeKey) {
        if (this.membershipInfoMap.isEmpty()) return null;
        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.higherEntry(nodeKey);

        return (closestNode == null)
                ? this.membershipInfoMap.firstEntry()
                : closestNode;
    }

    public void mergeTable(MembershipTable membershipTable) {
        var aux = membershipTable.getMembershipInfoMap();
        for (var key : aux.keySet()) {
            if (!this.membershipInfoMap.containsKey(key)) {
                this.membershipInfoMap.put(key, aux.get(key));
            }
        }
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        for (var key : this.membershipInfoMap.keySet()){
            ret.append(key).append(":");
            ret.append(this.membershipInfoMap.get(key).toString());
            ret.append("\n");
        }
        return ret.toString();
    }
}
