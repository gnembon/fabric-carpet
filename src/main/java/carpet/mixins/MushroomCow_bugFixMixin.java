package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MushroomCow.class)
public abstract class MushroomCow_bugFixMixin extends Mob {

    protected MushroomCow_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Cow;setInvulnerable(Z)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(SoundSource soundSource, CallbackInfo ci, Cow cow) {
        if (CarpetSettings.mobConvertKeepNBTTags) {
            this.getActiveEffects().forEach(cow::addEffect);
            cow.getAttributes().assignValues(this.getAttributes());
            cow.setDeltaMovement(this.getDeltaMovement());
            cow.setRemainingFireTicks(this.getRemainingFireTicks());
            cow.setNoAi(this.isNoAi());
            cow.setNoGravity(this.isNoGravity());
            cow.setPortalCooldown(this.getPortalCooldown());
            cow.setXRot(this.getXRot());
            cow.setYRot(this.getYRot());
            cow.setYBodyRot(this.getVisualRotationYInDegrees());
            cow.setYHeadRot(this.getYHeadRot());
            cow.setSilent(this.isSilent());
            this.getTags().forEach(cow::addTag);
            cow.setUUID(this.getUUID());
        }
    }
}
