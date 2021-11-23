package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER,
                    target = "net/minecraft/entity/ExperienceOrbEntity.pickingCount:I"
            )
    )
    void addXP(PlayerEntity player, CallbackInfo ci) {
        if (CarpetSettings.xpNoCooldown) {
            int remainder;
            while (this.pickingCount > 0) {
                remainder = this.repairPlayerGears(player, this.amount);
                if (remainder > 0) {
                    player.addExperience(remainder);
                }
                player.experiencePickUpDelay = 0;
                this.pickingCount--;
            }
        }
    }
}
