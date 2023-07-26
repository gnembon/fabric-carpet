package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Slime.class)
public abstract class Slime_bugFixMixin extends Mob {
    protected Slime_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(RemovalReason removalReason, CallbackInfo ci, int i, Component component, boolean bl, float f, int j, int k, int l, float g, float h, Slime slime) {
        if (CarpetSettings.mobConvertKeepNBTTagsFix) {
            this.getActiveEffects().forEach(slime::addEffect);
            slime.getAttributes().assignValues(this.getAttributes());
            slime.setDeltaMovement(this.getDeltaMovement());
            // slime.setRemainingFireTicks(this.getRemainingFireTicks()); technically a newly spawned entity, not the same one converted
            slime.setNoGravity(this.isNoGravity());
            // slime.setPortalCooldown(this.getPortalCooldown()); same here
            slime.setSilent(this.isSilent());
            this.getTags().forEach(slime::addTag);
        }
    }
}
