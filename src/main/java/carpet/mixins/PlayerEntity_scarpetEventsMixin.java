package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_TAKES_DAMAGE;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_scarpetEventsMixin extends LivingEntity
{
    protected PlayerEntity_scarpetEventsMixin(EntityType<? extends LivingEntity> type, World world)
    {
        super(type, world);
    }

    @Inject(method = "applyDamage", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;addExhaustion(F)V"
    ))
    private void playerTakingDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        // version of LivingEntity_scarpetEventsMixin::entityTakingDamage
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.EntityEventType.ON_DAMAGE, amount, source);
        if (PLAYER_TAKES_DAMAGE.isNeeded())
        {
            PLAYER_TAKES_DAMAGE.onDamage(this, amount, source);
        }
        if (source.getAttacker() instanceof ServerPlayerEntity && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            PLAYER_DEALS_DAMAGE.onDamage(this, amount, source);
        }
    }
}
