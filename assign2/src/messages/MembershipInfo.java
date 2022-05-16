package messages;

import java.util.Objects;

public class MembershipInfo {
    //TODO: GUARDAR DATA DA INFORMAÇÂO
    //TODO: GUARDAR ESTADO DO NÓ (ADORMECIDO OU NÂO)

    private String ip;
    private int port;

    public MembershipInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIP() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public static String getIPFromString(String rec){
        return rec.substring(rec.length() - 7);
    }

    public static String getPortFromString(String rec){
        return rec.substring(rec.length() - 5, rec.length()-1);
    }

    @Override
    public String toString(){
        return ip + ": " + port;
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
