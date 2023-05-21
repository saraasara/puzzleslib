package fuzs.puzzleslib.impl.client.core.context;

import com.google.common.base.Preconditions;
import fuzs.puzzleslib.api.client.core.v1.context.ColorProvidersContext;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public final class BlockColorProvidersContextFabricImpl implements ColorProvidersContext<Block, BlockColor> {

    @Override
    public void registerColorProvider(BlockColor provider, Block... blocks) {
        Objects.requireNonNull(provider, "provider is null");
        Objects.requireNonNull(blocks, "blocks is null");
        Preconditions.checkPositionIndex(0, blocks.length, "blocks is empty");
        for (Block block : blocks) {
            Objects.requireNonNull(block, "block is null");
            ColorProviderRegistry.BLOCK.register(provider, block);
        }
    }

    @Override
    public BlockColor getProviders() {
        return (BlockState blockState, BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, int i) -> {
            BlockColor blockColor = ColorProviderRegistry.BLOCK.get(blockState.getBlock());
            return blockColor == null ? -1 : blockColor.getColor(blockState, blockAndTintGetter, blockPos, i);
        };
    }
}
