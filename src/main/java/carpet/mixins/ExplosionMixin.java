package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Explosion.class)
public abstract class ExplosionMixin
{
    @Shadow
    @Final
    private List<BlockPos> affectedBlocks;
    
    @Inject(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z"))
    private void clearList(CallbackInfo ci)
    {
        if (CarpetSettings.explosionNoBlockDamage)
        {
            this.affectedBlocks.clear();
        }
    }
    
}
