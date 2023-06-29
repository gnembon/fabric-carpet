package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerLevel.class)
public class ServerLevel_spawnChunksMixin
{
    @ModifyConstant(method = "setDefaultSpawnPos", constant = @Constant(intValue = 11), expect = 2)
    private int pushLimit(int original)
    {
        return CarpetSettings.spawnChunksSize;
    }
}
