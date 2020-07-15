package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.EntityInterface;
import carpet.fakes.LivingEntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_TAKES_DAMAGE;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_scarpetEventsMixin extends Entity implements LivingEntityInterface
{

    @Shadow protected abstract void jump();

    @Shadow protected boolean jumping;

    public LivingEntity_scarpetEventsMixin(EntityType<?> type, World world)
    {
        super(type, world);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathCall(DamageSource damageSource_1, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.EntityEventType.ON_DEATH, damageSource_1.name);
    }

    @Inject(method = "applyDamage", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getHealth()F",
            shift = At.Shift.BEFORE
    ))
    private void entityTakingDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.EntityEventType.ON_DAMAGE, amount, source);
        // this is not applicable since its not a playr for sure
        //if (entity instanceof ServerPlayerEntity && PLAYER_TAKES_DAMAGE.isNeeded())
        //{
        //    PLAYER_TAKES_DAMAGE.onDamage(entity, float_2, damageSource_1);
        //}
        if (source.getAttacker() instanceof ServerPlayerEntity && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            PLAYER_DEALS_DAMAGE.onDamage(this, amount, source);
        }
    }

    @Override
    public void doJumpCM()
    {
        jump();
    }

    @Override
    public boolean isJumpingCM()
    {
        return jumping;
    }
}
