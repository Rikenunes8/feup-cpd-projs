package membership;

import java.util.*;
import java.util.stream.Collectors;

public class MembershipTable {

    private final TreeMap<String, MembershipInfo> membershipInfoMap;

    public MembershipTable() {
        this.membershipInfoMap = new TreeMap<>();
    }

    public void addMembershipInfo(String hashedId, MembershipInfo mInfo) {
        this.membershipInfoMap.put(hashedId, mInfo);
    }


    public void removeMembershipInfo(String hashedId) {
        this.membershipInfoMap.remove(hashedId);
    }

    public Map<String, String> getMembershipInfoList() {
        return this.membershipInfoMap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getValue().getIP(), Map.Entry::getKey));
    }

    public TreeMap<String, MembershipInfo> getMembershipInfoMap() {
        return this.membershipInfoMap;
    }

    public MembershipInfo getClosestMembershipInfo(String key) {
        if (this.membershipInfoMap.isEmpty()) return null;
        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.ceilingEntry(key);

        // the key is after the last hashedId in the tree
        return (closestNode == null)
                // : reassign the closest value as the first one in the tree
                ? this.membershipInfoMap.firstEntry().getValue()
                : closestNode.getValue();
    }

    public MembershipInfo getNextClosestMembershipInfo(String nodeKey) {
        if (this.membershipInfoMap.isEmpty()) return null;
        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.higherEntry(nodeKey);

        return (closestNode == null)
                ? this.membershipInfoMap.firstEntry().getValue()
                : closestNode.getValue();
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        for (MembershipInfo entry : this.membershipInfoMap.values()){
            ret.append(entry.toString());
            ret.append("\n");
        }
        return ret.toString();
    }

    public void mergeTable(MembershipTable membershipTable) {
        var aux = membershipTable.getMembershipInfoMap();
        for (var key : aux.keySet()) {
            if (!this.membershipInfoMap.containsKey(key)) {
                this.membershipInfoMap.put(key, aux.get(key));
            }
        }
    }

    public boolean hasStore(String keyHashed) {
        return this.membershipInfoMap.containsKey(keyHashed);
    }
}
