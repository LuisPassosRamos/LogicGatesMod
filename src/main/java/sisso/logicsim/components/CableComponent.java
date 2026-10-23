package sisso.logicsim.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Serializable marker so cables are discoverable again after section loading. */
public final class CableComponent implements Component<ChunkStore> {
    public static final BuilderCodec<CableComponent> CODEC =
            BuilderCodec.builder(CableComponent.class, CableComponent::new).build();

    @Override
    public CableComponent clone() { return new CableComponent(); }
}
