package fuzs.puzzleslib.impl.client.core.context;

import com.google.common.base.Preconditions;
import fuzs.puzzleslib.api.client.core.v1.context.ColorProvidersContext;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;

public final class ItemColorProvidersContextFabricImpl implements ColorProvidersContext<Item, ItemColor> {

    @Override
    public void registerColorProvider(ItemColor provider, Item... items) {
        Objects.requireNonNull(provider, "provider is null");
        Objects.requireNonNull(items, "items is null");
        Preconditions.checkPositionIndex(0, items.length, "items is empty");
        for (ItemLike item : items) {
            Objects.requireNonNull(item, "item is null");
            ColorProviderRegistry.ITEM.register(provider, item);
        }
    }

    @Override
    public ItemColor getProviders() {
        return (ItemStack itemStack, int i) -> {
            ItemColor itemColor = ColorProviderRegistry.ITEM.get(itemStack.getItem());
            return itemColor == null ? -1 : itemColor.getColor(itemStack, i);
        };
    }
}
