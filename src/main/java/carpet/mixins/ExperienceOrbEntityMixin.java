package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {
    @Shadow
    private int pickingCount;

    @Shadow
    private int amount;

    @Shadow
    protected abstract int repairPlayerGears(PlayerEntity player, int amount);

    @Inject(
            method = "onPlayerCollision",
            at = @At(
                    value = "FIELD",
                    target = "net/minecraft/entity/player/PlayerEntity.experiencePickUpDelay:I",
                    ordinal = 0
            )
    )
    private void addXP(PlayerEntity player, CallbackInfo ci) {
        if (CarpetSettings.xpNoCooldown) {
            player.experiencePickUpDelay = 0;
            // reducing the count to 1 and leaving vanilla to deal with it
            while (this.pickingCount > 1) {
                int remainder = this.repairPlayerGears(player, this.amount);
                if (remainder > 0) {
                    player.addExperience(remainder);
                }
                this.pickingCount--;
            }
        }
    }
}
