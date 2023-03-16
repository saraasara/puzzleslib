package fuzs.puzzleslib.init;

import fuzs.puzzleslib.core.ModLoader;
import fuzs.puzzleslib.init.builder.ExtendedMenuSupplier;
import fuzs.puzzleslib.init.builder.PoiTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * handles registering to game registries
 * this is a mod specific instance now for Fabric compatibility, Forge would support retrieving current namespace from mod loading context
 * originally based on RegistryHelper found in Vazkii's AutoRegLib mod
 */
public interface RegistryManager {

    /**
     * @return namespace for this instance
     */
    String namespace();

    /**
     * allows for registering content in the common project for only a few mod loaders
     *
     * @param allowedModLoaders the mod loaders to register on, every mod loader not registered to should handle this in the loader specific subproject
     * @return this manager as a builder
     */
    RegistryManager whenOn(ModLoader... allowedModLoaders);

    /**
     * allows for registering content in the common project for only a few mod loaders
     *
     * @param forbiddenModLoaders the mod loaders to not register on
     * @return this manager as a builder
     */
    default RegistryManager whenNotOn(ModLoader... forbiddenModLoaders) {
        ModLoader[] allowedModLoaders = Stream.of(ModLoader.values()).filter(modLoader -> !ArrayUtils.contains(forbiddenModLoaders, modLoader)).toArray(ModLoader[]::new);
        return this.whenOn(allowedModLoaders);
    }

    /**
     * allow for deferring registration on Fabric, required when e.g. registering blocks in Fabric project, but related block entity is registered in common
     * <p>follows the same order as Forge: blocks, items, everything else
     */
    default void applyRegistration() {
        // registration is always deferred on Forge, so make this have a default variant
    }

    /**
     * creates a placeholder registry reference for this {@link #namespace()}
     *
     * @param registryKey key for registry to register to
     * @param path        path for new entry
     * @param <T>         registry type
     * @return placeholder registry object for <code>entry</code>
     */
    default <T> RegistryReference<T> placeholder(final ResourceKey<? extends Registry<? super T>> registryKey, String path) {
        return RegistryReference.placeholder(registryKey, this.makeKey(path));
    }

    /**
     * register any type of registry entry with a path
     *
     * @param registryKey key for registry to register to
     * @param path        path for new entry
     * @param supplier    supplier for entry to register
     * @param <T>         registry type
     * @return registry object for <code>entry</code>
     */
    <T> RegistryReference<T> register(final ResourceKey<? extends Registry<? super T>> registryKey, String path, Supplier<T> supplier);

    /**
     * register block entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<Block> registerBlock(String path, Supplier<Block> entry) {
        return this.register(Registries.BLOCK, path, entry);
    }

    /**
     * register item entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<Item> registerItem(String path, Supplier<Item> entry) {
        return this.register(Registries.ITEM, path, entry);
    }

    /**
     * Registers a block item for a given block.
     *
     * @param blockReference reference for block to register item variant for
     * @return registry object for the new block item
     */
    default RegistryReference<Item> registerBlockItem(RegistryReference<Block> blockReference) {
        return this.registerBlockItem(blockReference, new Item.Properties());
    }

    /**
     * Registers a block item for a given block.
     *
     * @param blockReference reference for block to register item variant for
     * @param itemProperties properties for item
     * @return registry object for the new block item
     */
    default RegistryReference<Item> registerBlockItem(RegistryReference<Block> blockReference, Item.Properties itemProperties) {
        return this.registerItem(blockReference.getResourceLocation().getPath(), () -> new BlockItem(blockReference.get(), itemProperties));
    }

    /**
     * Registers a spawn egg item for a given entity type.
     *
     * @param entityTypeReference reference for the entity type to register a spawn egg for
     * @param backgroundColor     background color of the spawn egg item
     * @param highlightColor      spots color pf the spawn egg item
     * @return registry reference for the new spawn egg item
     */
    default RegistryReference<Item> registerSpawnEggItem(RegistryReference<EntityType<? extends Mob>> entityTypeReference, int backgroundColor, int highlightColor) {
        return this.registerSpawnEggItem(entityTypeReference, backgroundColor, highlightColor, new Item.Properties());
    }

    /**
     * Registers a spawn egg item for a given entity type.
     *
     * @param entityTypeReference reference for the entity type to register a spawn egg for
     * @param backgroundColor     background color of the spawn egg item
     * @param highlightColor      spots color pf the spawn egg item
     * @param itemProperties      properties for the item
     * @return registry reference for the new spawn egg item
     */
    RegistryReference<Item> registerSpawnEggItem(RegistryReference<EntityType<? extends Mob>> entityTypeReference, int backgroundColor, int highlightColor, Item.Properties itemProperties);

    /**
     * register fluid entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<Fluid> registerFluid(String path, Supplier<Fluid> entry) {
        return this.register(Registries.FLUID, path, entry);
    }

    /**
     * register mob effect entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<MobEffect> registerMobEffect(String path, Supplier<MobEffect> entry) {
        return this.register(Registries.MOB_EFFECT, path, entry);
    }

    /**
     * register sound event entry with a path
     *
     * @param path path for new entry
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<SoundEvent> registerSoundEvent(String path) {
        return this.register(Registries.SOUND_EVENT, path, () -> SoundEvent.createVariableRangeEvent(this.makeKey(path)));
    }

    /**
     * register potion entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<Potion> registerPotion(String path, Supplier<Potion> entry) {
        return this.register(Registries.POTION, path, entry);
    }

    /**
     * register enchantment entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<Enchantment> registerEnchantment(String path, Supplier<Enchantment> entry) {
        return this.register(Registries.ENCHANTMENT, path, entry);
    }

    /**
     * register entity type entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @param <T>   entity type parameter
     * @return registry object for <code>entry</code>
     */
    @SuppressWarnings("unchecked")
    default <T extends Entity> RegistryReference<EntityType<T>> registerEntityType(String path, Supplier<EntityType.Builder<T>> entry) {
        return this.register((ResourceKey<Registry<EntityType<T>>>) (ResourceKey<?>) Registries.ENTITY_TYPE, path, () -> entry.get().build(path));
    }

    /**
     * register tile entity type entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @param <T>   block entity type parameter
     * @return registry object for <code>entry</code>
     */
    @SuppressWarnings("unchecked")
    default <T extends BlockEntity> RegistryReference<BlockEntityType<T>> registerBlockEntityType(String path, Supplier<BlockEntityType.Builder<T>> entry) {
        return this.register((ResourceKey<Registry<BlockEntityType<T>>>) (ResourceKey<?>) Registries.BLOCK_ENTITY_TYPE, path, () -> entry.get().build(null));
    }

    /**
     * register container type entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @param <T>   container menu type parameter
     * @return registry object for <code>entry</code>
     */
    @SuppressWarnings("unchecked")
    default <T extends AbstractContainerMenu> RegistryReference<MenuType<T>> registerMenuType(String path, Supplier<MenuType.MenuSupplier<T>> entry) {
        return this.register((ResourceKey<Registry<MenuType<T>>>) (ResourceKey<?>) Registries.MENU, path, () -> new MenuType<>(entry.get()));
    }

    /**
     * register container type entry with a path
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @param <T>   container menu type
     * @return registry object for <code>entry</code>
     */
    <T extends AbstractContainerMenu> RegistryReference<MenuType<T>> registerExtendedMenuType(String path, Supplier<ExtendedMenuSupplier<T>> entry);

    /**
     * register poi type from custom builder
     *
     * @param path  path for new entry
     * @param entry supplier for entry to register
     * @return registry object for <code>entry</code>
     */
    RegistryReference<PoiType> registerPoiTypeBuilder(String path, Supplier<PoiTypeBuilder> entry);

    /**
     * register a new type of recipe
     *
     * @param path path for new entry
     * @param <T>  recipe type
     * @return registry object for <code>entry</code>
     */
    default <T extends Recipe<?>> RegistryReference<RecipeType<T>> registerRecipeType(String path) {
        return this.register(Registries.RECIPE_TYPE, path, () -> new RecipeType<>() {
            private final String id = RegistryManager.this.makeKey(path).toString();

            @Override
            public String toString() {
                return this.id;
            }
        });
    }

    /**
     * register a new game event that can be listened to
     *
     * @param path               path for new entry
     * @param notificationRadius range in blocks in which this event will be listened to
     * @return registry object for <code>entry</code>
     */
    default RegistryReference<GameEvent> registerGameEvent(String path, int notificationRadius) {
        return this.register(Registries.GAME_EVENT, path, () -> new GameEvent(path, notificationRadius));
    }

    /**
     * @param path path for location
     * @return resource location for {@link #namespace}
     */
    default ResourceLocation makeKey(String path) {
        if (StringUtils.isEmpty(path)) throw new IllegalArgumentException("Can't register object without name");
        return new ResourceLocation(this.namespace(), path);
    }
}
