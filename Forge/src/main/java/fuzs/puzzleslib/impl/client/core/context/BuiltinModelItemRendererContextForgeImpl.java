package fuzs.puzzleslib.impl.client.core.context;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.puzzleslib.api.client.core.v1.context.BuiltinModelItemRendererContext;
import fuzs.puzzleslib.api.client.init.v1.DynamicBuiltinItemRenderer;
import fuzs.puzzleslib.impl.client.core.ForwardingClientItemExtensions;
import fuzs.puzzleslib.mixin.client.accessor.ItemForgeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record BuiltinModelItemRendererContextForgeImpl(
        List<ResourceManagerReloadListener> dynamicRenderers) implements BuiltinModelItemRendererContext {

    @Override
    public void registerItemRenderer(DynamicBuiltinItemRenderer renderer, ItemLike... items) {
        // copied from Forge, seems to break data gen otherwise
        if (FMLLoader.getLaunchHandler().isData()) return;
        // do not check for ContentRegistrationFlags#BUILT_IN_ITEM_MODEL_RENDERERS being properly set as not every built-in item renderer needs to reload
        Objects.requireNonNull(renderer, "renderer is null");
        Objects.requireNonNull(items, "items is null");
        Preconditions.checkPositionIndex(0, items.length, "items is empty");
        IClientItemExtensions itemExtensions = new IClientItemExtensions() {
            @Nullable
            private BlockEntityWithoutLevelRenderer blockEntityWithoutLevelRenderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.blockEntityWithoutLevelRenderer == null) {
                    this.blockEntityWithoutLevelRenderer = new ForwardingBlockEntityWithoutLevelRenderer(Minecraft.getInstance(), renderer);
                }
                return this.blockEntityWithoutLevelRenderer;
            }
        };
        for (ItemLike item : items) {
            Objects.requireNonNull(item, "item is null");
            setClientItemExtensions(item, itemExtensions);
        }
        // store this to enable listening to resource reloads
        this.dynamicRenderers.add(renderer);
    }

    private static void setClientItemExtensions(ItemLike item, IClientItemExtensions itemExtensions) {
        // this solution is very dangerous as it relies on internal stuff in Forge
        // but there is no other way for multi-loader and without making this a huge inconvenience so ¯\_(ツ)_/¯
        Object renderProperties = ((ItemForgeAccessor) item.asItem()).puzzleslib$getRenderProperties();
        ((ItemForgeAccessor) item.asItem()).puzzleslib$setRenderProperties(renderProperties != null ? new ForwardingClientItemExtensions((IClientItemExtensions) renderProperties) {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return itemExtensions.getCustomRenderer();
            }
        } : itemExtensions);
    }

    private static class ForwardingBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
        private final DynamicBuiltinItemRenderer renderer;

        public ForwardingBlockEntityWithoutLevelRenderer(Minecraft minecraft, DynamicBuiltinItemRenderer renderer) {
            super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            this.renderer = renderer;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
            this.renderer.renderByItem(stack, mode, matrices, vertexConsumers, light, overlay);
        }
    }
}
