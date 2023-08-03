package carpet.fakes;

import java.util.Map;
import java.util.function.BooleanSupplier;

import carpet.helpers.ServerTickRateManager;
import carpet.script.CarpetScriptServer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
//import net.minecraft.server.ServerResources;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

public interface MinecraftServerInterface
{
    default void carpet$forceTick(BooleanSupplier isAhead) { throw new UnsupportedOperationException(); }

    default LevelStorageSource.LevelStorageAccess carpet$getStorageSource() { throw new UnsupportedOperationException(); }

    default Map<ResourceKey<Level>, ServerLevel> carpet$getLevels() { throw new UnsupportedOperationException(); }

    default void carpet$reloadAfterReload(RegistryAccess newRegs) { throw new UnsupportedOperationException(); }

    default MinecraftServer.ReloadableResources carpet$getResources() { throw new UnsupportedOperationException(); }

    default void carpet$addScriptServer(CarpetScriptServer scriptServer) { throw new UnsupportedOperationException(); }

    default CarpetScriptServer carpet$getScriptServer() { throw new UnsupportedOperationException(); }

    default ServerTickRateManager carpet$getTickRateManager() { throw new UnsupportedOperationException(); }
}
