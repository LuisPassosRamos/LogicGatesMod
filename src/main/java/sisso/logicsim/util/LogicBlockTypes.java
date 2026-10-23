package sisso.logicsim.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import sisso.logicsim.circuit.CircuitNode;

public final class LogicBlockTypes {
    public static final String STATE_ON = "On";
    public static final String STATE_OFF = "Off";
    public static final String LEVER_ID = "Logic_Lever";
    public static final String LAMP_ID = "Logic_Lamp";
    public static final String CABLE_ID = "Logic_Cable";
    public static final String AND_ID = "Logic_AND_Gate";
    public static final String OR_ID = "Logic_OR_Gate";
    public static final String NOT_ID = "Logic_NOT_Gate";
    public static final String NAND_ID = "Logic_NAND_Gate";
    public static final String NOR_ID = "Logic_NOR_Gate";
    public static final String XOR_ID = "Logic_XOR_Gate";
    public static final String XNOR_ID = "Logic_XNOR_Gate";

    private LogicBlockTypes() { }

    public static boolean isLogicLever(BlockType type) { return matches(type, LEVER_ID); }
    public static boolean isLogicLamp(BlockType type) { return matches(type, LAMP_ID); }
    public static boolean isLogicCable(BlockType type) { return matches(type, CABLE_ID); }
    public static boolean isLogicGate(BlockType type) {
        var kind = kind(type);
        return kind != null && kind.isGate();
    }

    public static CircuitNode.Kind kind(BlockType type) {
        if (isLogicLever(type)) return CircuitNode.Kind.LEVER;
        if (isLogicCable(type)) return CircuitNode.Kind.CABLE;
        if (isLogicLamp(type)) return CircuitNode.Kind.LAMP;
        if (matches(type, AND_ID)) return CircuitNode.Kind.AND;
        if (matches(type, OR_ID)) return CircuitNode.Kind.OR;
        if (matches(type, NOT_ID)) return CircuitNode.Kind.NOT;
        if (matches(type, NAND_ID)) return CircuitNode.Kind.NAND;
        if (matches(type, NOR_ID)) return CircuitNode.Kind.NOR;
        if (matches(type, XOR_ID)) return CircuitNode.Kind.XOR;
        if (matches(type, XNOR_ID)) return CircuitNode.Kind.XNOR;
        return null;
    }

    public static String stateName(BlockType type) {
        return type == null ? null : type.getStateForBlock(type);
    }

    public static String cableState(int mask) {
        if (mask < 0 || mask > 15) throw new IllegalArgumentException("Invalid cable mask: " + mask);
        return "Connections_" + mask;
    }

    public static String terminalState(boolean on, int localMask) {
        return (on ? STATE_ON : STATE_OFF) + "_" + cableState(localMask);
    }

    public static boolean isOnState(String state) {
        return STATE_ON.equals(state) || state != null && state.startsWith(STATE_ON + "_Connections_");
    }

    public static int connectionsInState(String state) {
        if (state == null) return 0;
        int prefix = state.indexOf("Connections_");
        if (prefix < 0) return 0;
        int mask = Integer.parseInt(state.substring(prefix + "Connections_".length()));
        if (mask < 0 || mask > 15) throw new IllegalArgumentException("Invalid connection state: " + state);
        return mask;
    }

    private static boolean matches(BlockType type, String assetId) {
        if (type == null) return false;
        if (assetId.equals(type.getId()) || assetId.equals(type.getDefaultStateKey())) return true;
        return type.getItem() != null && assetId.equals(type.getItem().getId());
    }
}
