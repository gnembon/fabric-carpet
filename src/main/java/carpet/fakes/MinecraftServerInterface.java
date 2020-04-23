package carpet.fakes;

import net.minecraft.world.level.storage.LevelStorage;

import java.util.function.BooleanSupplier;

public interface MinecraftServerInterface
{
    void forceTick(BooleanSupplier sup);
    LevelStorage.Session getCMSession();
}
