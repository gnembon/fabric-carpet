package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_TAKES_DAMAGE;

@Mixin(DamageTracker.class)
public abstract class DamageTracker_scarpetEventMixin
{
    @Shadow @Final private LivingEntity entity;

    @Inject(method = "onDamage", at = @At("HEAD"))
    private void onDamageTaken(DamageSource damageSource_1, float float_1, float float_2, CallbackInfo ci)
    {
        ((EntityInterface)entity).getEventContainer().onEvent(EntityEventsGroup.EntityEventType.ON_DAMAGE, entity, float_2, damageSource_1);
        if (entity instanceof ServerPlayerEntity && PLAYER_TAKES_DAMAGE.isNeeded())
        {
            PLAYER_TAKES_DAMAGE.onDamage(entity, float_2, damageSource_1);
        }
        if (damageSource_1.getAttacker() instanceof ServerPlayerEntity && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            PLAYER_DEALS_DAMAGE.onDamage(entity, float_2, damageSource_1);
        }
    }
}
