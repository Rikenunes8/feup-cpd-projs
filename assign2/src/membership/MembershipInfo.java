package membership;

import java.util.Objects;

public class MembershipInfo {
    //TODO: GUARDAR DATA DA INFORMAÇÂO
    //TODO: GUARDAR ESTADO DO NÓ (ADORMECIDO OU NÂO)

    private final String ip;
    private final int port;

    public MembershipInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    public MembershipInfo(String ipAndPort) {
        this.ip = getIPFromString(ipAndPort);
        this.port = getPortFromString(ipAndPort);
    }

    public String getIP() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public static String getIPFromString(String rec){
        return rec.split(":")[0].trim();
    }

    public static int getPortFromString(String rec){
        return Integer.parseInt(rec.split(":")[1].trim());
    }

    @Override
    public String toString(){
        return this.ip + ":" + this.port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembershipInfo that = (MembershipInfo) o;
        return this.port == that.port && Objects.equals(this.ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
