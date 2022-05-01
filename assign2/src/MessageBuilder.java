import java.util.List;

public class MessageBuilder {
    public static String messageJoinLeave(int nodeIP, int port, int membershipCounter) {
        // TODO implement
        return null;
    }

    public static String messageMembership() {
        // TODO implement
        return null;
    }

    private static String buildHeader(List<String> headerLines) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String line : headerLines) {
            stringBuilder.append(line);
            stringBuilder.append(0xD);
            stringBuilder.append(0xA);
        }
        stringBuilder.append(0xD);
        stringBuilder.append(0xA);
        return stringBuilder.toString();
    }

}
