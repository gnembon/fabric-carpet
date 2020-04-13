package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerWorld.class)
public class ServerWorld_spawnChunksMixin
{
    @ModifyConstant(method = "setSpawnPos", constant = @Constant(intValue = 11), expect = 2)
    private int pushLimit(int original)
    {
        return CarpetSettings.spawnChunksSize;
    }
}
