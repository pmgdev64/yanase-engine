package vn.pmgteam.yanase.network;

public enum PacketType {
    HANDSHAKE((byte) 0x00),
    MODULE_TOGGLE((byte) 0x01), // Trigger autoSave
    POSITION_UPDATE((byte) 0x02),
    DISCONNECT((byte) 0xFF);

    private final byte id;
    PacketType(byte id) { this.id = id; }
    public byte getId() { return id; }

    // Cache để lookup cực nhanh, không tạo object rác (Zero-Allocation)
    private static final PacketType[] LOOKUP = new PacketType[256];
    static {
        for (PacketType pt : values()) LOOKUP[pt.id & 0xFF] = pt;
    }
    public static PacketType fromId(byte id) { return LOOKUP[id & 0xFF]; }
}