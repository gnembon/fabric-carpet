package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Villager.class)
public abstract class Villager_bugFixMixin extends Mob {


    protected Villager_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Witch;setPersistenceRequired()V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(ServerLevel serverLevel, LightningBolt lightningBolt, CallbackInfo ci, Witch witch) {
        if (CarpetSettings.mobConvertKeepNBTTagsFix) {
            this.getActiveEffects().forEach(witch::addEffect);
            witch.getAttributes().assignValues(this.getAttributes());
            witch.setCanPickUpLoot(this.canPickUpLoot());
            witch.setInvulnerable(this.isInvulnerable());
            witch.setLeftHanded(this.isLeftHanded());
            witch.setNoGravity(this.isNoGravity());
            witch.setXRot(this.getXRot());
            witch.setYRot(this.getYRot());
            witch.setYBodyRot(this.getVisualRotationYInDegrees());
            witch.setYHeadRot(this.getYHeadRot());
            witch.setSilent(this.isSilent());
            this.getTags().forEach(witch::addTag);
        }
    }
}
