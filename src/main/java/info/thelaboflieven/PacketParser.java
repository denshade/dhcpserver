package info.thelaboflieven;

import info.thelaboflieven.dhcp.DHCPRequest;
import info.thelaboflieven.dhcp.HardWareAddressType;
import info.thelaboflieven.dhcp.MessageOpCode;

public class PacketParser {

    public DHCPRequest parse(byte[] receiveData) {
        MessageOpCode opcode = MessageOpCode.fromByte(receiveData[0]);
        HardWareAddressType hardWareAddressType = HardWareAddressType.fromByte(receiveData[1]);
        return new DHCPRequest(opcode, hardWareAddressType, null, (byte)0, 0, (short)0, null, null, null, null, null, null, null, null);
    }

}
