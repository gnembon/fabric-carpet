package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractPiglin.class)
public abstract class AbstractPiglin_bugFixMixin extends Mob {

    protected AbstractPiglin_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "finishConversion", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(ServerLevel serverLevel, CallbackInfo ci, ZombifiedPiglin zombifiedPiglin) {
        if (CarpetSettings.mobConvertKeepNBTTags) {
            this.getActiveEffects().forEach(zombifiedPiglin::addEffect);
            zombifiedPiglin.getAttributes().assignValues(this.getAttributes());
            zombifiedPiglin.setCanPickUpLoot(this.canPickUpLoot());
            zombifiedPiglin.setRemainingFireTicks(this.getRemainingFireTicks());
            zombifiedPiglin.setNoGravity(this.isNoGravity());
            zombifiedPiglin.setPortalCooldown(this.getPortalCooldown());
            zombifiedPiglin.setXRot(this.getXRot());
            zombifiedPiglin.setYRot(this.getYRot());
            zombifiedPiglin.setYBodyRot(this.getVisualRotationYInDegrees());
            zombifiedPiglin.setYHeadRot(this.getYHeadRot());
            zombifiedPiglin.setSilent(this.isSilent());
            this.getTags().forEach(zombifiedPiglin::addTag);
            zombifiedPiglin.setUUID(this.getUUID());
        }
    }
}
