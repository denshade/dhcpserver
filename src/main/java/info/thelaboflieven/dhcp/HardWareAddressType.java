package info.thelaboflieven.dhcp;

public enum HardWareAddressType {
        ETHERNET(1),
        EXPERIMENTAL_ETHERNET(2),
        AMATEUR_RADIO_AX25(3),
        PROTEON_TOKEN_RING(4),
        CHAOS(5),
        IEEE_802(6),
        ARCNET(7),
        HYPERCHANNEL(8),
        LANSTAR(9),
        AUTONET_SHORT_ADDRESS(10),
        LOCAL_TALK(11),
        LOCALNET(12),
        ULTRA_LINK(13),
        SMDS(14),
        FRAME_RELAY(15),
        ATM(16),
        HDLC(17),
        FIBRE_CHANNEL(18),
        SERIAL_LINE(20);

        private final int value;

        HardWareAddressType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
}
