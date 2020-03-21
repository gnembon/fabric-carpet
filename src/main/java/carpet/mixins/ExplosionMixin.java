package carpet.mixins;

import carpet.helpers.OptimizedExplosion;
import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = Explosion.class)
public abstract class ExplosionMixin
{
    @Shadow
    @Final
    private List<BlockPos> affectedBlocks;

    @Inject(method = "collectBlocksAndDamageEntities", at = @At("HEAD"),
            cancellable = true)
    private void onExplosionA(CallbackInfo ci)
    {
        if (CarpetSettings.optimizedTNT)
        {
            OptimizedExplosion.doExplosionA((Explosion) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "affectWorld", at = @At("HEAD"),
            cancellable = true)
    private void onExplosionB(boolean spawnParticles, CallbackInfo ci)
    {
        if (CarpetSettings.explosionNoBlockDamage)
        {
            affectedBlocks.clear();
        }
        if (CarpetSettings.optimizedTNT)
        {
            OptimizedExplosion.doExplosionB((Explosion) (Object) this, spawnParticles);
            ci.cancel();
        }
    }
    //optional due to Overwrite in Lithium
    //should kill most checks if no block damage is requested
    @Redirect(method = "collectBlocksAndDamageEntities", require = 0, at = @At(value = "INVOKE",
            target ="Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState noBlockCalcsWithNoBLockDamage(World world, BlockPos pos)
    {
        if (CarpetSettings.explosionNoBlockDamage) return Blocks.BEDROCK.getDefaultState();
        return world.getBlockState(pos);
    }
}
