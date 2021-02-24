package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_ATTACKS_ENTITY;
import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_INTERACTS_WITH_ENTITY;
import static carpet.script.CarpetEventServer.Event.PLAYER_TAKES_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_COLLIDES_WITH_ENTITY;

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
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DAMAGE, amount, source);
        if (PLAYER_TAKES_DAMAGE.isNeeded())
        {
            PLAYER_TAKES_DAMAGE.onDamage(this, amount, source);
        }
        if (source.getAttacker() instanceof ServerPlayerEntity && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            PLAYER_DEALS_DAMAGE.onDamage(this, amount, source);
        }
    }

    @Inject(method = "collideWithEntity", at = @At("HEAD"))
    private void onEntityCollision(Entity entity, CallbackInfo ci)
    {
        if (PLAYER_COLLIDES_WITH_ENTITY.isNeeded() && !world.isClient)
        {
            PLAYER_COLLIDES_WITH_ENTITY.onEntityHandAction((ServerPlayerEntity)(Object)this, entity, null);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void doInteract(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir)
    {
        if (!world.isClient && PLAYER_INTERACTS_WITH_ENTITY.isNeeded())
        {
            PLAYER_INTERACTS_WITH_ENTITY.onEntityHandAction((ServerPlayerEntity) (Object)this, entity, hand);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci)
    {
        if (!world.isClient && PLAYER_ATTACKS_ENTITY.isNeeded() && target.isAttackable())
        {
            PLAYER_ATTACKS_ENTITY.onEntityHandAction((ServerPlayerEntity) (Object)this, target, null);
        }
    }
}
