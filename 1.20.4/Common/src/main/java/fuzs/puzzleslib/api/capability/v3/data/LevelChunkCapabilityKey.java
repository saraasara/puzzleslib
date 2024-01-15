package fuzs.puzzleslib.api.capability.v3.data;

import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus;

/**
 * Convenient {@link CapabilityKey} implementation for {@link LevelChunk}.
 *
 * @param <C> capability component type
 */
@ApiStatus.NonExtendable
public interface LevelChunkCapabilityKey<C extends CapabilityComponent<LevelChunk>> extends CapabilityKey<LevelChunk, C> {

    @Override
    default void setChanged(C capabilityComponent) {
        capabilityComponent.getHolder().setUnsaved(true);
    }
}
