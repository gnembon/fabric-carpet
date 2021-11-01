package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true)
@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {

    @Shadow
    private int pickingCount;

    @Shadow
    private PlayerEntity target;

    @Shadow
    private int amount;

    @Unique
    private int mergeCoolDown = 50;

    public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        ExperienceOrbEntity thisOrb = (ExperienceOrbEntity) (Object) this;
        if (thisOrb.age % 20 == 0 && this.target == null) {
            this.target = this.world.getClosestPlayer(this, 8.0D);
        }

        if (mergeCoolDown > 0) {
            mergeCoolDown--;
        }
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "net/minecraft/entity/ExperienceOrbEntity.age:I"))
    public int redirectAge(ExperienceOrbEntity orb) {
        if (CarpetSettings.combineXPOrbs.equals("none")) {
            return 0;
        }
        if (CarpetSettings.combineXPOrbs.equals("carpet")) {
            return 1;
        }
        return orb.age;
    }

    @Inject(method = "isMergeable(Lnet/minecraft/entity/ExperienceOrbEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void changeValidOrbCondition(ExperienceOrbEntity othrOrb, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetSettings.combineXPOrbs.equals("carpet")) {
            ExperienceOrbEntity thisOrb = (ExperienceOrbEntity) (Object) this;
            cir.setReturnValue(
                    thisOrb != othrOrb                                               &&
                    thisOrb.getExperienceAmount() < 15000                            &&
                    othrOrb.getExperienceAmount() < 15000                            &&
                    ((ExperienceOrbEntityMixin) (Object) othrOrb).mergeCoolDown == 0 &&
                    this.mergeCoolDown == 0
            );
        }
    }

    @Inject(method = "merge", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, target = "net/minecraft/entity/ExperienceOrbEntity.pickingCount:I"))
    public void onMerge(@NotNull ExperienceOrbEntity otherOrb, CallbackInfo ci) {
        if (CarpetSettings.combineXPOrbs.equals("carpet")) {
            ExperienceOrbEntityMixin that = (ExperienceOrbEntityMixin) (Object) otherOrb;
            int thisXP = this.amount * this.pickingCount;
            int thatXP = that.amount * that.pickingCount;
            this.amount = thisXP + thatXP;
            this.pickingCount = 1;
        }
    }

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
