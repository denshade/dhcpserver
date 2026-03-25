package info.thelaboflieven.dhcp;

import java.util.Arrays;

/**
 * Minimal DHCP parsing/building: DISCOVER → OFFER, REQUEST → ACK.
 */
public final class DhcpPacketHandler {

    public static final int OPTIONS_OFFSET = 240;
    private static final byte COOKIE_0 = (byte) 99;
    private static final byte COOKIE_1 = (byte) 130;
    private static final byte COOKIE_2 = 83;
    private static final byte COOKIE_3 = 99;

    public static final int OPTION_REQUESTED_IP = 50;
    public static final int OPTION_DHCP_MESSAGE_TYPE = 53;
    public static final int OPTION_SERVER_IDENTIFIER = 54;
    public static final int DHCPDISCOVER = 1;
    public static final int DHCPOFFER = 2;
    public static final int DHCPREQUEST = 3;
    public static final int DHCPACK = 5;

    private DhcpPacketHandler() {
    }

    public static boolean hasDhcpMagicCookie(byte[] packet, int length) {
        return length >= OPTIONS_OFFSET + 4
                && packet[OPTIONS_OFFSET] == COOKIE_0
                && packet[OPTIONS_OFFSET + 1] == COOKIE_1
                && packet[OPTIONS_OFFSET + 2] == COOKIE_2
                && packet[OPTIONS_OFFSET + 3] == COOKIE_3;
    }

    /**
     * @return DHCP message type from option 53, or -1 if absent / invalid
     */
    public static int getDhcpMessageType(byte[] packet, int length) {
        if (!hasDhcpMagicCookie(packet, length)) {
            return -1;
        }
        int i = OPTIONS_OFFSET + 4;
        while (i < length) {
            int code = packet[i] & 0xFF;
            if (code == 255) {
                break;
            }
            if (code == 0) {
                i++;
                continue;
            }
            if (i + 1 >= length) {
                break;
            }
            int optLen = packet[i + 1] & 0xFF;
            if (code == OPTION_DHCP_MESSAGE_TYPE) {
                if (optLen >= 1 && i + 2 < length) {
                    return packet[i + 2] & 0xFF;
                }
                return -1;
            }
            i += 2 + optLen;
        }
        return -1;
    }

    /**
     * First occurrence of a DHCP option payload after the magic cookie, or null if missing / out of range.
     */
    public static byte[] getOption(byte[] packet, int length, int optionCode) {
        if (!hasDhcpMagicCookie(packet, length)) {
            return null;
        }
        int i = OPTIONS_OFFSET + 4;
        while (i < length) {
            int code = packet[i] & 0xFF;
            if (code == 255) {
                break;
            }
            if (code == 0) {
                i++;
                continue;
            }
            if (i + 1 >= length) {
                break;
            }
            int optLen = packet[i + 1] & 0xFF;
            if (code == optionCode) {
                if (i + 2 + optLen > length) {
                    return null;
                }
                return Arrays.copyOfRange(packet, i + 2, i + 2 + optLen);
            }
            i += 2 + optLen;
        }
        return null;
    }

    /**
     * If the packet is a DHCPDISCOVER, builds a DHCPOFFER reply (BOOTREPLY) with the same xid and chaddr.
     *
     * @param serverIdentifier 4 octets, network byte order (e.g. 127.0.0.1)
     * @param offeredYiaddr    4 octets offered client address (yiaddr field)
     * @return offer bytes, or null if not a discover
     */
    public static byte[] createOfferIfDiscover(
            byte[] request,
            int length,
            byte[] serverIdentifier,
            byte[] offeredYiaddr) {
        if (length < OPTIONS_OFFSET + 4 || request.length < length) {
            return null;
        }
        if ((request[0] & 0xFF) != 1) {
            return null;
        }
        if (getDhcpMessageType(request, length) != DHCPDISCOVER) {
            return null;
        }
        if (serverIdentifier.length != 4 || offeredYiaddr.length != 4) {
            throw new IllegalArgumentException("serverIdentifier and offeredYiaddr must be 4 bytes");
        }

        int outLen = Math.max(300, length);
        byte[] reply = Arrays.copyOf(request, outLen);
        reply[0] = 2;
        Arrays.fill(reply, 12, 28, (byte) 0);
        System.arraycopy(offeredYiaddr, 0, reply, 16, 4);

        int o = OPTIONS_OFFSET;
        reply[o++] = COOKIE_0;
        reply[o++] = COOKIE_1;
        reply[o++] = COOKIE_2;
        reply[o++] = COOKIE_3;
        reply[o++] = (byte) OPTION_DHCP_MESSAGE_TYPE;
        reply[o++] = 1;
        reply[o++] = (byte) DHCPOFFER;
        reply[o++] = (byte) OPTION_SERVER_IDENTIFIER;
        reply[o++] = 4;
        System.arraycopy(serverIdentifier, 0, reply, o, 4);
        o += 4;
        reply[o++] = (byte) 255;
        while (o < reply.length) {
            reply[o++] = 0;
        }
        return reply;
    }

    /**
     * If the packet is a DHCPREQUEST for this server (option 54 matches {@code serverIdentifier}),
     * builds a DHCPACK with yiaddr taken from option 50 (requested IP).
     *
     * @return ack bytes, or null if not a matching request
     */
    public static byte[] createAckIfRequest(
            byte[] request,
            int length,
            byte[] serverIdentifier) {
        if (length < OPTIONS_OFFSET + 4 || request.length < length) {
            return null;
        }
        if ((request[0] & 0xFF) != 1) {
            return null;
        }
        if (getDhcpMessageType(request, length) != DHCPREQUEST) {
            return null;
        }
        if (serverIdentifier.length != 4) {
            throw new IllegalArgumentException("serverIdentifier must be 4 bytes");
        }
        byte[] reqServer = getOption(request, length, OPTION_SERVER_IDENTIFIER);
        if (reqServer == null || reqServer.length != 4 || !Arrays.equals(reqServer, serverIdentifier)) {
            return null;
        }
        byte[] requestedIp = getOption(request, length, OPTION_REQUESTED_IP);
        if (requestedIp == null || requestedIp.length != 4) {
            return null;
        }

        int outLen = Math.max(300, length);
        byte[] reply = Arrays.copyOf(request, outLen);
        reply[0] = 2;
        Arrays.fill(reply, 12, 28, (byte) 0);
        System.arraycopy(requestedIp, 0, reply, 16, 4);

        int o = OPTIONS_OFFSET;
        reply[o++] = COOKIE_0;
        reply[o++] = COOKIE_1;
        reply[o++] = COOKIE_2;
        reply[o++] = COOKIE_3;
        reply[o++] = (byte) OPTION_DHCP_MESSAGE_TYPE;
        reply[o++] = 1;
        reply[o++] = (byte) DHCPACK;
        reply[o++] = (byte) OPTION_SERVER_IDENTIFIER;
        reply[o++] = 4;
        System.arraycopy(serverIdentifier, 0, reply, o, 4);
        o += 4;
        reply[o++] = (byte) 255;
        while (o < reply.length) {
            reply[o++] = 0;
        }
        return reply;
    }
}
