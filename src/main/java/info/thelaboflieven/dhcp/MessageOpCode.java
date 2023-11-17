package info.thelaboflieven.dhcp;

public enum MessageOpCode {
    BOOTREQUEST((byte) 1), BOOTREPLY((byte) 2);
    private final byte code;

    MessageOpCode(byte code) {
        this.code = code;
    }
    public static MessageOpCode fromByte(byte code) {
        for (MessageOpCode opCode : MessageOpCode.values()) {
            if (opCode.code == code) {
                return opCode;
            }
        }
        throw new IllegalArgumentException("Invalid MessageOpCode byte value: " + code);
    }
}
