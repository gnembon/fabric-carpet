package carpet.fakes;

import java.util.Map;
import java.util.function.BooleanSupplier;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
//import net.minecraft.server.ServerResources;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

public interface MinecraftServerInterface
{
    void forceTick(BooleanSupplier sup);
    LevelStorageSource.LevelStorageAccess getCMSession();
    Map<ResourceKey<Level>, ServerLevel> getCMWorlds();
    void reloadAfterReload(RegistryAccess newRegs);

    MinecraftServer.ReloadableResources getResourceManager();
}
