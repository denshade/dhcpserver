package info.thelaboflieven;

import info.thelaboflieven.dhcp.DhcpPacketHandler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DHCPServer {
    private static final int DHCP_SERVER_PORT = 1067;
    private static final int DHCP_PACKET_SIZE = 548;

    /** Default offered address when running {@link #main}; tests may use other values. */
    static final byte[] DEFAULT_OFFER_YIADDR = {(byte) 192, (byte) 168, 0, 10};

    static final byte[] DEFAULT_SERVER_ID = {127, 0, 0, 1};

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(DHCP_SERVER_PORT)) {
            byte[] receiveData = new byte[DHCP_PACKET_SIZE];
            System.out.println("DHCP server running...");

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                respondToDiscoverIfPresent(socket, receivePacket, DEFAULT_SERVER_ID, DEFAULT_OFFER_YIADDR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * If the datagram is a DHCPDISCOVER, sends a DHCPOFFER back to the sender's address and port.
     */
    static void respondToDiscoverIfPresent(
            DatagramSocket socket,
            DatagramPacket receivePacket,
            byte[] serverIdentifier,
            byte[] offeredYiaddr) throws java.io.IOException {
        byte[] data = receivePacket.getData();
        int len = receivePacket.getLength();
        byte[] offer = DhcpPacketHandler.createOfferIfDiscover(data, len, serverIdentifier, offeredYiaddr);
        if (offer == null) {
            return;
        }
        DatagramPacket sendPacket = new DatagramPacket(
                offer,
                offer.length,
                receivePacket.getAddress(),
                receivePacket.getPort());
        socket.send(sendPacket);
    }
}
