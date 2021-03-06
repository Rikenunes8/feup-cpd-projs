package client;

import store.IMembership;
import messages.MessageClient;
import messages.TcpMessager;
import utils.HashUtils;

import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.regex.Pattern;

public class TestClient {
    public static void main(String[] args) throws NotBoundException, IOException {
        String correctInput = """
                java TestClient <node_ap> <operation> [<opnd>]\s
                  <node_ap> : IP:PORT
                  <operation> : key-value (put, get or delete) or membership (join, leave)
                  <opnd> [needed for key-value operations] :
                       put: file pathname to the value to be added
                       get or delete: string of hexadecimal symbols encoding the sha-256 key returned by put""";

        if (args.length < 2 || args.length > 4) {
            System.out.println("Wrong number of expected arguments. \n" + correctInput);
            return;
        }

        if (!isValidNodeAccessPoint(args[0])) {
            System.out.println("Wrong node access point representation. \n" + correctInput);
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
                Registry registry = LocateRegistry.getRegistry();
                IMembership service = (IMembership) registry.lookup(nodeAC);
                service.join();
            }
            case "leave" -> {
                System.out.println("perform leave operation nodeAC = " + nodeAC);
                Registry registry = LocateRegistry.getRegistry();
                IMembership service = (IMembership) registry.lookup(nodeAC);
                service.leave();
            }
            case "put" -> {
                if (args.length != 3) {
                    System.out.println("Excepted 2 Operation Argument since is a PUT key-value operation. \n" + correctInput);
                    return;
                }
                String filename = args[2];
                System.out.println("perform put operation nodeAC= " + nodeAC + " , filename= " + filename);
                String value = readFile(filename);
                if (value == null) break;
                String key = HashUtils.getHashedSha256(value);
                if (key == null) break;

                try (Socket socket = new Socket(nodeIP, nodePort)) {
                    TcpMessager.sendMessage(socket, MessageClient.putMessage(key, value));
                    String response = TcpMessager.receiveMessage(socket);
                    System.out.println(new MessageClient(response).getBody());
                }
                System.out.println("\nKey: " + key);
            }
            case "get" -> {
                if (args.length != 3) {
                    System.out.println("Excepted 1 Operation Argument since is a GET key-value operation. \n" + correctInput);
                    return;
                }
                String key = args[2];
                System.out.println("perform get operation nodeAC= " + nodeAC + " , key= " + key);

                try (Socket socket = new Socket(nodeIP, nodePort)) {
                    TcpMessager.sendMessage(socket, MessageClient.getMessage(key));
                    String value = TcpMessager.receiveMessage(socket);
                    System.out.println("\n\nValue:\n");
                    System.out.println(new MessageClient(value).getBody());
                }
            }
            case "delete" -> {
                if (args.length != 3) {
                    System.out.println("Excepted 1 Operation Argument since is a DELETE key-value operation. \n" + correctInput);
                    return;
                }
                String key = args[2];
                System.out.println("perform delete operation nodeAC= " + nodeAC + " , key= " + key);
                try (Socket socket = new Socket(nodeIP, nodePort)) {
                    TcpMessager.sendMessage(socket, MessageClient.deleteMessage(key));
                    String response = TcpMessager.receiveMessage(socket);
                    System.out.println(new MessageClient(response).getBody());
                }
            }
            default -> System.out.println("Specified Operation does not exists. \n" + correctInput);
        }
    }

    // Function to validate the entry for node access point.
    public static boolean isValidNodeAccessPoint(String nodeAP) {
        String regexZeroTo255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
        String regexIP = regexZeroTo255 + "\\." + regexZeroTo255 + "\\." + regexZeroTo255 + "\\." + regexZeroTo255;
        String regexPORT = "(\\d{1,4}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])";
        String regex = regexIP + "\\:" + regexPORT;

        Pattern pattern = Pattern.compile(regex);
        return (pattern.matcher(nodeAP).matches());
    }

    private static String readFile(String pathname) {
        File keyFile = new File(pathname);
        if (!keyFile.exists()) {
            System.out.println("File name " + pathname + " does not exist");
            return null;
        }
        if (!keyFile.canRead()){
            System.out.println("Permission denied! Can not read file named " + pathname);
            return null;
        }
        try {
            Scanner sc = new Scanner(keyFile);
            StringBuilder value = new StringBuilder();
            while (sc.hasNextLine()) {
                value.append(sc.nextLine());
            }
            return value.toString();
        } catch (FileNotFoundException e) {
            System.out.println("Error reading file " + pathname);
            return null;
        }
    }
}
