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

    public String getClosestMembershipInfo(String key) {
        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.ceilingEntry(key);
        // the key is after the last hashedId in the tree
        if (closestNode == null) {
            // : reassign the closest value as the first one in the tree
            closestNode = this.membershipInfoMap.firstEntry();
        }

        // return in IP:PORT format
        return closestNode.getValue().toString();
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
}
