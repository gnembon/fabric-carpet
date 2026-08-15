package carpet.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static carpet.script.CarpetEventServer.Event.BLOCK_FORMS;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_scarpetEventMixin extends Level
{
    protected ServerLevel_scarpetEventMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i)
    {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    private boolean onBlockFormed(ServerLevel level, BlockPos pos, BlockState newState)
    {
        if (BLOCK_FORMS.isNeeded())
        {
            BlockState previous = level.getBlockState(pos);
            if (BLOCK_FORMS.onBlockForms(level, pos, previous, newState))
            {
                return false;
            }
        }
        return level.setBlockAndUpdate(pos, newState);
    }

    @Redirect(method = "tickPrecipitation", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0
    ))
    private boolean onIceForm(ServerLevel level, BlockPos pos, BlockState newState)
    {
        return onBlockFormed(level, pos, newState);
    }

    @Redirect(method = "tickPrecipitation", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 1
    ))
    private boolean onSnowLayerGrow(ServerLevel level, BlockPos pos, BlockState newState)
    {
        return onBlockFormed(level, pos, newState);
    }

    @Redirect(method = "tickPrecipitation", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 2
    ))
    private boolean onSnowForm(ServerLevel level, BlockPos pos, BlockState newState)
    {
        return onBlockFormed(level, pos, newState);
    }
}
