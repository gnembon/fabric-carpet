package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Tadpole.class)
public abstract class Tadpole_bugFixMixin extends Mob {
    protected Tadpole_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "ageUp()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(CallbackInfo ci, ServerLevel serverLevel, Frog frog) {
        if (CarpetSettings.mobConvertKeepNBTTags) {
            frog.setXRot(this.getXRot());
            frog.setYRot(this.getYRot());
            frog.setYBodyRot(this.getVisualRotationYInDegrees());
            frog.setYHeadRot(this.getYHeadRot());
            frog.setUUID(this.getUUID());
        }
    }
}
