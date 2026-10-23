package sisso.logicsim.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import sisso.logicsim.util.LogicBlockTypes;

public final class LeverComponent implements Component<ChunkStore> {
    public static final BuilderCodec<LeverComponent> CODEC = BuilderCodec
            .builder(LeverComponent.class, LeverComponent::new)
            .append(new KeyedCodec<>("On", Codec.BOOLEAN),
                    (component, value) -> {
                        component.on = Boolean.TRUE.equals(value);
                        component.initialized = true;
                    }, component -> component.on).add().build();
    private boolean on;
    private transient boolean initialized;

    public boolean isOn() { return on; }
    public void toggle() { on = !on; initialized = true; }

    public void restoreFromStoredVisualStateIfNeeded(BlockType blockType) {
        if (!initialized) {
            on = LogicBlockTypes.isOnState(LogicBlockTypes.stateName(blockType));
            initialized = true;
        }
    }

    @Override
    public LeverComponent clone() {
        var copy = new LeverComponent();
        copy.on = on;
        copy.initialized = initialized;
        return copy;
    }
}
