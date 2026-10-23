package sisso.logicsim.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.util.BlockComponentAccess.GateTopology;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Transient resource owned by one world's ChunkStore and accessed on its thread. */
public final class CircuitWorldState implements Resource<ChunkStore> {
    public final Map<Ref<ChunkStore>, BlockPosition> liveNodes = new HashMap<>();
    public final Set<Ref<ChunkStore>> sections = new HashSet<>();
    public final Map<Ref<ChunkStore>, Map<Short, Holder<ChunkStore>>> parkedNodes = new HashMap<>();
    public final Map<BlockPosition, GateTopology> gateTopologies = new HashMap<>();
    public boolean dirty = true;
    public boolean scheduled;
    public int nodeCount;
    public int poweredCableCount;
    public int litLampCount;
    public int gateCount;
    public int activeGateCount;
    public int pendingGateCount;
    public int feedbackGateCount;
    public long evaluations;

    public void invalidate() { dirty = true; }

    @Override
    public CircuitWorldState clone() {
        // Runtime indices must be reconstructed, never shared between worlds.
        return new CircuitWorldState();
    }
}
