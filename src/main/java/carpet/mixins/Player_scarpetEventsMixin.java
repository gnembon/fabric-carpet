package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

@Mixin(Player.class)
public abstract class Player_scarpetEventsMixin extends LivingEntity
{
    protected Player_scarpetEventsMixin(EntityType<? extends LivingEntity> type, Level world)
    {
        super(type, world);
    }

    @Inject(method = "actuallyHurt", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
    ))
    private void playerTakingDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        // version of LivingEntity_scarpetEventsMixin::entityTakingDamage
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DAMAGE, amount, source);
        if (PLAYER_TAKES_DAMAGE.isNeeded())
        {
            if(PLAYER_TAKES_DAMAGE.onDamage(this, amount, source)) {
                ci.cancel();
            }
        }
        if (source.getEntity() instanceof ServerPlayer && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            if(PLAYER_DEALS_DAMAGE.onDamage(this, amount, source)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "touch", at = @At("HEAD"))
    private void onEntityCollision(Entity entity, CallbackInfo ci)
    {
        if (PLAYER_COLLIDES_WITH_ENTITY.isNeeded() && !level.isClientSide)
        {
            PLAYER_COLLIDES_WITH_ENTITY.onEntityHandAction((ServerPlayer)(Object)this, entity, null);
        }
    }

    @Inject(method = "interactOn", cancellable = true, at = @At("HEAD"))
    private void doInteract(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (!level.isClientSide && PLAYER_INTERACTS_WITH_ENTITY.isNeeded())
        {
            if(PLAYER_INTERACTS_WITH_ENTITY.onEntityHandAction((ServerPlayer) (Object)this, entity, hand)) {
                cir.setReturnValue(InteractionResult.PASS);
                cir.cancel();
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci)
    {
        if (!level.isClientSide && PLAYER_ATTACKS_ENTITY.isNeeded() && target.isAttackable())
        {
            if(PLAYER_ATTACKS_ENTITY.onEntityHandAction((ServerPlayer) (Object)this, target, null)) {
                ci.cancel();
            }
        }
    }
}
