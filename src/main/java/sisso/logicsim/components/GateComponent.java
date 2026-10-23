package sisso.logicsim.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Persistent marker only. Gate power and connections are always derived. */
public final class GateComponent implements Component<ChunkStore> {
    public static final BuilderCodec<GateComponent> CODEC =
            BuilderCodec.builder(GateComponent.class, GateComponent::new).build();

    @Override
    public GateComponent clone() { return new GateComponent(); }
}
