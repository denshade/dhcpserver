package info.thelaboflieven;

import java.net.*;

public class DHCPServer {
    private static final int DHCP_SERVER_PORT = 1067;
    private static final int DHCP_CLIENT_PORT = 68;
    private static final int DHCP_PACKET_SIZE = 548;
    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(DHCP_SERVER_PORT);
            byte[] receiveData = new byte[DHCP_PACKET_SIZE];

            System.out.println("DHCP server running...");

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                socket.receive(receivePacket);
                System.out.println(new String(receiveData));
                /*
                // Process received DHCP packet
                DHCP_Packet requestPacket = processReceivedPacket(receivePacket.getData());

                // Generate DHCP response packet
                DHCP_Packet responsePacket = generateResponsePacket(requestPacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                DatagramPacket sendPacket = new DatagramPacket(
                        serializePacket(responsePacket), serializePacket(responsePacket).length,
                        clientAddress, clientPort);

                // Send DHCP response to the client
                socket.send(sendPacket);
                */
                // Handle DHCP lease allocation, IP assignment, etc.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
