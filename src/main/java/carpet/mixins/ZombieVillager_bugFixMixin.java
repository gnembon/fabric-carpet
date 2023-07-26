package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillager_bugFixMixin extends Mob {

    protected ZombieVillager_bugFixMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }


    @Inject(method = "finishConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EquipmentSlot;values()[Lnet/minecraft/world/entity/EquipmentSlot;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void mixin(ServerLevel serverLevel, CallbackInfo ci, Villager villager) {
        if (CarpetSettings.mobConvertKeepNBTTagsFix) {
            this.getActiveEffects().forEach(villager::addEffect);
            villager.setCanPickUpLoot(this.canPickUpLoot());
            villager.setRemainingFireTicks(this.getRemainingFireTicks());
            villager.setLeftHanded(this.isLeftHanded());
            villager.setNoGravity(this.isNoGravity());
            villager.setPortalCooldown(this.getPortalCooldown());
            villager.setSilent(this.isSilent());
            this.getTags().forEach(villager::addTag);
            villager.setUUID(this.getUUID());
        }
    }
}
