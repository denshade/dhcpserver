package info.thelaboflieven;

import info.thelaboflieven.dhcp.DHCPRequest;
import info.thelaboflieven.dhcp.HardWareAddressType;
import info.thelaboflieven.dhcp.MessageOpCode;

public class PacketParser {

    public DHCPRequest parse(byte[] receiveData) {
        MessageOpCode opcode = MessageOpCode.fromByte(receiveData[0]);
        HardWareAddressType hardWareAddressType = HardWareAddressType.fromByte(receiveData[1]);
        int hardwareLength = receiveData[2];
        byte hops = receiveData[3];
        return new DHCPRequest(opcode, hardWareAddressType, hardwareLength, (byte)0, 0, (short)0, null, null, null, null, null, null, null, null);
    }

}
