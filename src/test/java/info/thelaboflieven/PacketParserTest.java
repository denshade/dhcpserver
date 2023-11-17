package info.thelaboflieven;

import info.thelaboflieven.dhcp.HardWareAddressType;
import info.thelaboflieven.dhcp.MessageOpCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketParserTest {

    @Test
    void parse() {
        byte[] dhcpRequest = new byte[]{
                0x01, 0x01, 0x06, 0x00,  // Message Type: DHCP Request
                0x01, 0x42, (byte) 0x8f, (byte) 0x99,  // Client Hardware Address: 01:42:8f:99 (example)
                // Other DHCP request parameters...
                // (Options such as requested IP address, subnet mask, server identifier, etc.)
        };
        var request = new  PacketParser().parse(dhcpRequest);
        assertEquals(request.messageOpCode(), MessageOpCode.BOOTREQUEST);
        assertEquals(request.hardWareAddressType(), HardWareAddressType.ETHERNET);
    }
}