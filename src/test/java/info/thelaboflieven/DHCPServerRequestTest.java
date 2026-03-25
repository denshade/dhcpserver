package info.thelaboflieven;

import info.thelaboflieven.dhcp.DhcpPacketHandler;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DHCPServerRequestTest {

    private static final byte[] SERVER_ID = {127, 0, 0, 1};
    private static final byte[] OTHER_SERVER_ID = {10, 0, 0, 1};
    private static final byte[] REQUESTED_IP = {(byte) 192, (byte) 168, 0, 99};

    @Test
    void serverRepliesToDhcpRequestWithAck() throws Exception {
        CountDownLatch serverReady = new CountDownLatch(1);
        AtomicInteger serverPort = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        Thread serverThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(0)) {
                serverPort.set(socket.getLocalPort());
                serverReady.countDown();
                byte[] buf = new byte[548];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                socket.receive(recv);
                DHCPServer.respondToRequestIfPresent(socket, recv, SERVER_ID);
            } catch (Exception e) {
                failure.incrementAndGet();
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverReady.await(5, TimeUnit.SECONDS));
        assertEquals(0, failure.get());

        int xid = 0x99aabbcc;
        byte[] mac = {6, 5, 4, 3, 2, 1};
        byte[] request = minimalDhcpRequest(xid, mac, REQUESTED_IP, SERVER_ID);

        try (DatagramSocket client = new DatagramSocket(0)) {
            client.setSoTimeout(5000);
            InetAddress loopback = InetAddress.getLoopbackAddress();
            client.send(new DatagramPacket(request, request.length, loopback, serverPort.get()));

            byte[] responseBuf = new byte[548];
            DatagramPacket response = new DatagramPacket(responseBuf, responseBuf.length);
            client.receive(response);

            assertEquals(0, failure.get());
            byte[] data = response.getData();
            int len = response.getLength();

            assertEquals(2, data[0] & 0xFF, "BOOTREPLY");
            assertEquals(xid, readUint32(data, 4));
            assertArrayEquals(REQUESTED_IP, slice(data, 16, 4), "yiaddr from option 50");
            assertArrayEquals(mac, slice(data, 28, 6), "chaddr");
            assertEquals(DhcpPacketHandler.DHCPACK, DhcpPacketHandler.getDhcpMessageType(data, len));
            assertOptionServerId(data, len, SERVER_ID);
        }

        serverThread.join(2000);
    }

    @Test
    void handlerBuildsAckForMinimalRequest() {
        int xid = 0x11223344;
        byte[] req = minimalDhcpRequest(xid, new byte[]{1, 1, 1, 1, 1, 1}, REQUESTED_IP, SERVER_ID);
        byte[] ack = DhcpPacketHandler.createAckIfRequest(req, req.length, SERVER_ID);
        assertNotNull(ack);
        assertEquals(2, ack[0] & 0xFF);
        assertEquals(xid, readUint32(ack, 4));
        assertArrayEquals(REQUESTED_IP, slice(ack, 16, 4));
        assertEquals(DhcpPacketHandler.DHCPACK, DhcpPacketHandler.getDhcpMessageType(ack, 300));
    }

    @Test
    void requestForWrongServerReturnsNull() {
        byte[] req = minimalDhcpRequest(1, new byte[]{1, 2, 3, 4, 5, 6}, REQUESTED_IP, OTHER_SERVER_ID);
        assertNull(DhcpPacketHandler.createAckIfRequest(req, req.length, SERVER_ID));
    }

    @Test
    void requestWithoutOption50ReturnsNull() {
        byte[] p = new byte[300];
        p[0] = 1;
        p[1] = 1;
        p[2] = 6;
        writeUint32(p, 4, 0xabc);
        System.arraycopy(new byte[]{9, 9, 9, 9, 9, 9}, 0, p, 28, 6);
        int o = DhcpPacketHandler.OPTIONS_OFFSET;
        p[o++] = (byte) 99;
        p[o++] = (byte) 130;
        p[o++] = 83;
        p[o++] = 99;
        p[o++] = (byte) DhcpPacketHandler.OPTION_DHCP_MESSAGE_TYPE;
        p[o++] = 1;
        p[o++] = (byte) DhcpPacketHandler.DHCPREQUEST;
        p[o++] = (byte) DhcpPacketHandler.OPTION_SERVER_IDENTIFIER;
        p[o++] = 4;
        System.arraycopy(SERVER_ID, 0, p, o, 4);
        o += 4;
        p[o++] = (byte) 255;
        assertNull(DhcpPacketHandler.createAckIfRequest(p, o, SERVER_ID));
    }

    private static byte[] minimalDhcpRequest(int xid, byte[] mac6, byte[] requestedIp, byte[] serverId) {
        byte[] p = new byte[300];
        p[0] = 1;
        p[1] = 1;
        p[2] = 6;
        p[3] = 0;
        writeUint32(p, 4, xid);
        System.arraycopy(mac6, 0, p, 28, 6);
        int o = DhcpPacketHandler.OPTIONS_OFFSET;
        p[o++] = (byte) 99;
        p[o++] = (byte) 130;
        p[o++] = 83;
        p[o++] = 99;
        p[o++] = (byte) DhcpPacketHandler.OPTION_DHCP_MESSAGE_TYPE;
        p[o++] = 1;
        p[o++] = (byte) DhcpPacketHandler.DHCPREQUEST;
        p[o++] = (byte) DhcpPacketHandler.OPTION_REQUESTED_IP;
        p[o++] = 4;
        System.arraycopy(requestedIp, 0, p, o, 4);
        o += 4;
        p[o++] = (byte) DhcpPacketHandler.OPTION_SERVER_IDENTIFIER;
        p[o++] = 4;
        System.arraycopy(serverId, 0, p, o, 4);
        o += 4;
        p[o++] = (byte) 255;
        return p;
    }

    private static void writeUint32(byte[] b, int offset, int v) {
        b[offset] = (byte) ((v >>> 24) & 0xff);
        b[offset + 1] = (byte) ((v >>> 16) & 0xff);
        b[offset + 2] = (byte) ((v >>> 8) & 0xff);
        b[offset + 3] = (byte) (v & 0xff);
    }

    private static int readUint32(byte[] b, int offset) {
        return ((b[offset] & 0xff) << 24)
                | ((b[offset + 1] & 0xff) << 16)
                | ((b[offset + 2] & 0xff) << 8)
                | (b[offset + 3] & 0xff);
    }

    private static byte[] slice(byte[] b, int from, int len) {
        byte[] out = new byte[len];
        System.arraycopy(b, from, out, 0, len);
        return out;
    }

    private static void assertOptionServerId(byte[] packet, int length, byte[] expectedFour) {
        int i = DhcpPacketHandler.OPTIONS_OFFSET + 4;
        while (i < length) {
            int code = packet[i] & 0xFF;
            if (code == 255) {
                break;
            }
            if (code == 0) {
                i++;
                continue;
            }
            int optLen = packet[i + 1] & 0xFF;
            if (code == DhcpPacketHandler.OPTION_SERVER_IDENTIFIER) {
                assertEquals(4, optLen);
                assertArrayEquals(expectedFour, slice(packet, i + 2, 4));
                return;
            }
            i += 2 + optLen;
        }
        throw new AssertionError("option 54 not found");
    }
}
