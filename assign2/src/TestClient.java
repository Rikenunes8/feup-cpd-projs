import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

public class TestClient {

    public static void main(String[] args) {
        String correctInput = """
                java TestClient <node_ap> <operation> [<opnd>]\s
                  <node_ap> : IP:PORT (UDP or TCP) | name of the remote object (RMI)
                  <operation> : key-value (put, get or delete) or membership (join, leave)
                  <opnd> [needed for key-value operations] :
                       put: file pathname and value to add
                       get or delete: string of hexadecimal symbols encoding the sha-256 key returned by put""";

        if (args.length < 2 || args.length > 4) {
            System.out.println("Wrong number of expected arguments. \n" + correctInput);
            return;
        }

        if (!isValidNodeAccessPoint("UDP", args[0])) {
            System.out.println("Wrong node access point representation according to the implementation. \n" + correctInput);
            return;
        }

        String nodeAC = args[0];
        String operation = args[1];
        String[] nodeACsep = nodeAC.split(":");
        String nodeIP = nodeACsep[0];
        int nodePort = Integer.parseInt(nodeACsep[1]);

        switch (operation) {
            case "join" -> {
                System.out.println("perform join operation nodeAC = " + nodeAC);
                try (Socket socket = new Socket(nodeIP, nodePort)) {
                    OutputStream output = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(output, true);
                    writer.println("join");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case "leave" ->{
                System.out.println("perform leave operation nodeAC = " + nodeAC);
                try (Socket socket = new Socket(nodeIP, nodePort)) {
                    OutputStream output = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(output, true);
                    writer.println("leave");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case "put" -> {
                if (args.length != 4) {
                    System.out.println("Excepted 2 Operation Argument since is a PUT key-value operation. \n" + correctInput);
                    return;
                }
                String key = args[2];
                String value = args[3];
                System.out.println("perform join operation nodeAC= " + nodeAC + " , key= " + key + " , value= " + value);
            }
            case "get" -> {
                if (args.length != 3) {
                    System.out.println("Excepted 1 Operation Argument since is a GET key-value operation. \n" + correctInput);
                    return;
                }
                String encoded = args[2];
                System.out.println("perform get operation nodeAC= " + nodeAC + " , encoded= " + encoded);
            }
            case "delete" -> {
                if (args.length != 3) {
                    System.out.println("Excepted 1 Operation Argument since is a DELETE key-value operation. \n" + correctInput);
                    return;
                }
                String encoded = args[2];
                System.out.println("perform delete operation nodeAC= " + nodeAC + " , encoded= " + encoded);
            }
            default -> System.out.println("Specified Operation does not exists. \n" + correctInput);
        }
    }

    // Function to validate the entry for node access point.
    public static boolean isValidNodeAccessPoint(String implementation, String nodeAP) {
        if (implementation.equals("TCP") || implementation.equals("UDP")) {
            String regexZeroTo255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
            String regexIP = regexZeroTo255 + "\\." + regexZeroTo255 + "\\." + regexZeroTo255 + "\\." + regexZeroTo255;
            String regexPORT = "(\\d{1,4}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])";
            String regex = regexIP + "\\:" + regexPORT;

            Pattern pattern = Pattern.compile(regex);
            return (pattern.matcher(nodeAP).matches());
        } else if(implementation.equals("RMI")) {
            return true;
        } else {
            System.out.println("Not a valid implementation parameter");
            return false;
        }
    }
}
