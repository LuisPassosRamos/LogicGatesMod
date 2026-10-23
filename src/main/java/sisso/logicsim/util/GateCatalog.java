package sisso.logicsim.util;

import com.google.gson.JsonParser;
import sisso.logicsim.circuit.CircuitNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Runtime identifiers shared with generators; no Hytale world access. */
public final class GateCatalog {
    public record Gate(CircuitNode.Kind kind, String itemId, String folder, int allowedMask) { }
    private static final Map<CircuitNode.Kind, Gate> GATES = load();
    private static final List<Gate> ORDERED = List.copyOf(GATES.values());
    private GateCatalog() { }

    public static Gate get(CircuitNode.Kind kind) {
        var gate = GATES.get(kind);
        if (gate == null) throw new IllegalArgumentException("Not a gate: " + kind);
        return gate;
    }

    public static List<Gate> gates() { return ORDERED; }

    private static Map<CircuitNode.Kind, Gate> load() {
        var result = new EnumMap<CircuitNode.Kind, Gate>(CircuitNode.Kind.class);
        try (var input = GateCatalog.class.getResourceAsStream("/logicgates/gate-catalog.json")) {
            if (input == null) throw new IllegalStateException("Gate catalog missing");
            var json = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("schemaVersion").getAsInt() != 1) throw new IllegalStateException("Unknown gate schema");
            for (var entry : json.getAsJsonArray("gates")) {
                var value = entry.getAsJsonObject();
                var kind = CircuitNode.Kind.valueOf(value.get("kind").getAsString());
                int mask = value.get("allowedMask").getAsInt();
                if (!kind.isGate() || value.get("inverting").getAsBoolean() != kind.isInverting()
                        || value.get("nonMonotonic").getAsBoolean() != kind.isNonMonotonic()
                        || mask != (kind == CircuitNode.Kind.NOT ? 10 : 15)) {
                    throw new IllegalStateException("Gate catalog differs from simulation: " + kind);
                }
                if (result.put(kind, new Gate(kind, value.get("itemId").getAsString(),
                        value.get("folder").getAsString(), mask)) != null) {
                    throw new IllegalStateException("Duplicate gate " + kind);
                }
            }
            for (var kind : CircuitNode.Kind.values()) if (kind.isGate() && !result.containsKey(kind)) {
                throw new IllegalStateException("Missing gate " + kind);
            }
            return java.util.Collections.unmodifiableMap(result);
        } catch (IOException error) { throw new IllegalStateException("Cannot read gate catalog", error); }
    }
}
