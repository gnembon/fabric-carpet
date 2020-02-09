package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin_portalDelayMixin
{
    @Final @Shadow public PlayerAbilities abilities;

    @Inject(method = "getMaxNetherPortalTime()I", at = @At("HEAD"), cancellable = true)
    private void onMaxNetherPortalTime(CallbackInfoReturnable<Integer> cir) {
        if(CarpetSettings.portalCreativeDelay != 1 && this.abilities.invulnerable) cir.setReturnValue(CarpetSettings.portalCreativeDelay);
        else if(CarpetSettings.portalSurvivalDelay != 80 && !this.abilities.invulnerable) cir.setReturnValue(CarpetSettings.portalSurvivalDelay);
    }
}
