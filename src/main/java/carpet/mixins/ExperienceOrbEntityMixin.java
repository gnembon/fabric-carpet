package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {
    @Shadow
    private int pickingCount;

    @Redirect(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"))
    void addXP(PlayerEntity player, int experience) {
        if (CarpetSettings.xpNoCooldown) {
            player.experiencePickUpDelay = 0;
            player.addExperience(experience * this.pickingCount);
            this.pickingCount = 1;
        } else {
            player.addExperience(experience);
        }
    }
}
