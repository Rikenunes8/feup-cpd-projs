package membership;

import java.util.*;

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

    public Set<MembershipInfo> getMembershipInfoList() {
        return new HashSet<>(this.membershipInfoMap.values());
    }

    public TreeMap<String, MembershipInfo> getMembershipInfoMap() {
        return this.membershipInfoMap;
    }

    public MembershipInfo getClosestMembershipInfo(String key) {
        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.ceilingEntry(key);

        // the key is after the last hashedId in the tree
        return (closestNode == null)
                // : reassign the closest value as the first one in the tree
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
}
