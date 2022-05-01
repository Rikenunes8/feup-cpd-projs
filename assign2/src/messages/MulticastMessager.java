package messages;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastMessager {
    public static String receiveMcastMessage(DatagramSocket datagramSocket) throws IOException {
        byte[] recvBuffer = new byte[8092];
        DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
        System.out.println("\nWaiting for a new packet: ...");
        datagramSocket.receive(recvPacket);
        return new String(recvPacket.getData(), 0, recvPacket.getLength());
    }
    public static void sendMcastMessage(String msg, DatagramSocket datagramSocket) throws IOException {
        InetAddress mcastAddr = datagramSocket.getInetAddress();
        int mcastPort = datagramSocket.getPort();
        if (msg == null) return;
        byte[] sndBuffer = msg.getBytes();
        DatagramPacket sndPacket = new DatagramPacket(sndBuffer, sndBuffer.length, mcastAddr, mcastPort);
        System.out.println("Sending message to " + mcastAddr + ":" + mcastPort);
        datagramSocket.send(sndPacket);
    }
}
