package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Zombie.class)
public abstract class Zombie_bugFixMixin extends Mob {
    protected Zombie_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "convertToZombieType", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Zombie;canBreakDoors()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(EntityType<? extends Zombie> entityType, CallbackInfo ci, Zombie zombie) {
        if (CarpetSettings.mobConvertKeepNBTTags) {
            this.getActiveEffects().forEach(zombie::addEffect);
            zombie.setRemainingFireTicks(this.getRemainingFireTicks());
            zombie.setLeftHanded(this.isLeftHanded());
            zombie.setNoGravity(this.isNoGravity());
            zombie.setPortalCooldown(this.getPortalCooldown());
            zombie.setXRot(this.getXRot());
            zombie.setYRot(this.getYRot());
            zombie.setYBodyRot(this.getVisualRotationYInDegrees());
            zombie.setYHeadRot(this.getYHeadRot());
            zombie.setSilent(this.isSilent());
            this.getTags().forEach(zombie::addTag);
            zombie.setUUID(this.getUUID());
        }
    }
}
