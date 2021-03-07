package carpet.fakes;

import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;

import java.util.Map;
import java.util.function.BooleanSupplier;

public interface MinecraftServerInterface
{
    void forceTick(BooleanSupplier sup);
    LevelStorage.Session getCMSession();
    Map<RegistryKey<World>, ServerWorld> getCMWorlds();
    ServerResourceManager getResourceManager();
}
