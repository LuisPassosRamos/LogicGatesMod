package sisso.logicsim.circuit;

/** Shared physical port compatibility for simulation and model connections. */
public final class CircuitConnections {
    private CircuitConnections() { }

    public static boolean isOutput(CircuitNode node, HorizontalDirection direction) {
        return node.kind() == CircuitNode.Kind.LEVER || node.isGate() && node.outputDirection() == direction;
    }

    public static boolean isInput(CircuitNode node, HorizontalDirection direction) {
        return node.kind() == CircuitNode.Kind.LAMP || node.isGate()
                && (CircuitNode.inputMask(node.kind(), node.outputDirection()) & direction.bit()) != 0;
    }

    private static boolean hasPort(CircuitNode node, HorizontalDirection direction) {
        return node.kind() == CircuitNode.Kind.CABLE || isInput(node, direction) || isOutput(node, direction);
    }

    public static boolean canConnect(CircuitNode first, HorizontalDirection towardSecond, CircuitNode second) {
        if (first == null || second == null) return false;
        if (!hasPort(first, towardSecond) || !hasPort(second, towardSecond.opposite())) return false;
        if (first.kind() == CircuitNode.Kind.CABLE || second.kind() == CircuitNode.Kind.CABLE) return true;
        return isOutput(first, towardSecond) != isOutput(second, towardSecond.opposite());
    }
}
