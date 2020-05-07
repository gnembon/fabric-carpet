package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.EntityInterface;
import carpet.fakes.LivingEntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_scarpetEventsMixin extends Entity implements LivingEntityInterface
{

    @Shadow protected abstract void jump();

    @Shadow protected boolean jumping;

    @Shadow public float flyingSpeed;

    public LivingEntity_scarpetEventsMixin(EntityType<?> type, World world)
    {
        super(type, world);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathCall(DamageSource damageSource_1, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.EntityEventType.ON_DEATH, this, damageSource_1.name);
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


    @ModifyConstant(method = "travel", constant = @Constant(floatValue = 0.91F), expect = 2)
    private float drag(float original)
    {
        if ((Object)this instanceof PlayerEntity)
        {
            PlayerEntity self = (PlayerEntity)(Object)(this);
            if (self.abilities.flying && !onGround)
                return (float)CarpetSettings.creativeFlyDrag;
        }
        return original;
    }


    @Inject(method = "getMovementSpeed(F)F", at = @At("HEAD"), cancellable = true)
    private void flyingAltSpeed(float slipperiness, CallbackInfoReturnable<Float> cir)
    {
        if ((Object)this instanceof PlayerEntity)
        {
            PlayerEntity self = (PlayerEntity)(Object)(this);
            if (self.abilities.flying && !onGround)
                cir.setReturnValue(flyingSpeed* (float)CarpetSettings.creativeFlySpeed);
        }
    }
}
