package fuzs.puzzleslib.impl.core;

import fuzs.puzzleslib.api.core.v1.CommonAbstractions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.BiConsumer;

public final class ForgeAbstractions implements CommonAbstractions {

    @Override
    public void openMenu(ServerPlayer player, MenuProvider menuProvider, BiConsumer<ServerPlayer, FriendlyByteBuf> screenOpeningDataWriter) {
        NetworkHooks.openScreen(player, menuProvider, buf -> screenOpeningDataWriter.accept(player, buf));
    }

    @Override
    public boolean isBossMob(EntityType<?> type) {
        return type.is(Tags.EntityTypes.BOSSES);
    }
}
