package fuzs.puzzleslib.client.core;

import fuzs.puzzleslib.PuzzlesLib;
import fuzs.puzzleslib.api.client.event.ModelEvents;
import fuzs.puzzleslib.client.init.builder.ModScreenConstructor;
import fuzs.puzzleslib.client.init.builder.ModSpriteParticleRegistration;
import fuzs.puzzleslib.mixin.client.accessor.MinecraftAccessor;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * wrapper class for {@link ClientModConstructor} for calling all required registration methods at the correct time,
 * which means everything is called immediately on Fabric (but in the correct order)
 * we use this wrapper style to allow for already registered to be used within the registration methods instead of having to use suppliers
 * (this doesn't really matter on Fabric)
 */
public class FabricClientModConstructor {

    /**
     * @param constructor the common mod main class implementation
     */
    private FabricClientModConstructor(ClientModConstructor constructor) {
        // only call ModConstructor::onConstructMod during object construction to be similar to Forge
        constructor.onConstructMod();
    }

    @SuppressWarnings("unchecked")
    private <T extends TooltipComponent> void registerClientTooltipComponent(Class<T> type, Function<? super T, ? extends ClientTooltipComponent> factory) {
        TooltipComponentCallback.EVENT.register((TooltipComponent data) -> {
            if (data.getClass() == type) return factory.apply((T) data);
            return null;
        });
    }

    private ClientModConstructor.ParticleProvidersContext getParticleProvidersContext() {
        return new ClientModConstructor.ParticleProvidersContext() {

            @Override
            public <T extends ParticleOptions> void registerParticleProvider(ParticleType<T> type, ParticleProvider<T> provider) {
                ParticleFactoryRegistry.getInstance().register(type, provider);
            }

            @Override
            public <T extends ParticleOptions> void registerParticleFactory(ParticleType<T> type, ModSpriteParticleRegistration<T> factory) {
                ParticleFactoryRegistry.getInstance().register(type, factory::create);
            }
        };
    }

    private <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenuScreen(MenuType<? extends M> menuType, ModScreenConstructor<M, U> factory) {
        MenuScreens.register(menuType, factory::create);
    }

    private void registerAtlasSprite(ResourceLocation atlasId, ResourceLocation spriteId) {
        ClientSpriteRegistryCallback.event(atlasId).register((TextureAtlas atlasTexture, ClientSpriteRegistryCallback.Registry registry) -> {
            registry.register(spriteId);
        });
    }

    private void registerLayerDefinition(ModelLayerLocation layerLocation, Supplier<LayerDefinition> supplier) {
        EntityModelLayerRegistry.registerModelLayer(layerLocation, supplier::get);
    }

    private <T> void registerSearchTree(SearchRegistry.Key<T> searchRegistryKey, SearchRegistry.TreeBuilderSupplier<T> treeBuilder) {
        ((MinecraftAccessor) Minecraft.getInstance()).getSearchRegistry().register(searchRegistryKey, treeBuilder);
    }

    private ClientModConstructor.ItemModelPropertiesContext getItemPropertiesContext() {
        return new ClientModConstructor.ItemModelPropertiesContext() {

            @Override
            public void register(ResourceLocation name, ClampedItemPropertyFunction property) {
                ItemProperties.registerGeneric(name, property);
            }

            @Override
            public void registerItem(Item item, ResourceLocation name, ClampedItemPropertyFunction property) {
                ItemProperties.register(item, name, property);
            }
        };
    }

    /**
     * construct the mod, calling all necessary registration methods (we don't need the object, it's only useful on Forge)
     *
     * @param modId         the mod id for registering events on Forge to the correct mod event bus
     * @param constructor   mod base class
     */
    public static void construct(String modId, ClientModConstructor constructor) {
        if (Strings.isBlank(modId)) throw new IllegalArgumentException("modId cannot be empty");
        PuzzlesLib.LOGGER.info("Constructing client components for mod {}", modId);
        FabricClientModConstructor fabricClientModConstructor = new FabricClientModConstructor(constructor);
        // everything after this is done on Forge using events called by the mod event bus
        // this is done since Forge works with loading stages, Fabric doesn't have those stages, so everything is called immediately
        constructor.onClientSetup();
        constructor.onRegisterEntityRenderers(EntityRendererRegistry::register);
        constructor.onRegisterBlockEntityRenderers(BlockEntityRendererRegistry::register);
        constructor.onRegisterClientTooltipComponents(fabricClientModConstructor::registerClientTooltipComponent);
        constructor.onRegisterParticleProviders(fabricClientModConstructor.getParticleProvidersContext());
        constructor.onRegisterMenuScreens(fabricClientModConstructor::registerMenuScreen);
        constructor.onRegisterAtlasSprites(fabricClientModConstructor::registerAtlasSprite);
        constructor.onRegisterLayerDefinitions(fabricClientModConstructor::registerLayerDefinition);
        constructor.onRegisterSearchTrees(fabricClientModConstructor::registerSearchTree);
        ModelEvents.BAKING_COMPLETED.register((ModelManager modelManager, Map<ResourceLocation, BakedModel> models, ModelBakery modelBakery) -> constructor.onLoadModels(new ClientModConstructor.LoadModelsContext(modelManager, models, modelBakery)));
        ModelLoadingRegistry.INSTANCE.registerModelProvider((ResourceManager manager, Consumer<ResourceLocation> out) -> {
            constructor.onRegisterAdditionalModels(out::accept);
        });
        constructor.onRegisterItemModelProperties(fabricClientModConstructor.getItemPropertiesContext());
    }
}