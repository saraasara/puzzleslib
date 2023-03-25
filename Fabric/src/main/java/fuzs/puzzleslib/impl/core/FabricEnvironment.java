package fuzs.puzzleslib.impl.core;

import fuzs.puzzleslib.api.core.v1.DistType;
import fuzs.puzzleslib.api.core.v1.FabricDistTypeConverter;
import fuzs.puzzleslib.api.core.v1.ModLoader;
import fuzs.puzzleslib.api.core.v1.ModLoaderEnvironment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.nio.file.Path;
import java.util.Optional;

/**
 * implementation of {@link ModLoaderEnvironment} for Fabric
 */
public final class FabricEnvironment implements ModLoaderEnvironment {

    @Override
    public ModLoader getModLoader() {
        return ModLoader.FABRIC;
    }

    @Override
    public DistType getEnvironmentType() {
        return FabricDistTypeConverter.fromEnvType(FabricLoader.getInstance().getEnvironmentType());
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Optional<String> getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(ModContainer::getMetadata).map(ModMetadata::getName);
    }
}