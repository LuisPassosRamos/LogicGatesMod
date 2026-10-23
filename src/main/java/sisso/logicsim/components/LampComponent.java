package sisso.logicsim.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Lamp output is derived from the current network, not a saved power source. */
public final class LampComponent implements Component<ChunkStore> {
    public static final BuilderCodec<LampComponent> CODEC =
            BuilderCodec.builder(LampComponent.class, LampComponent::new).build();

    @Override
    public LampComponent clone() { return new LampComponent(); }
}
