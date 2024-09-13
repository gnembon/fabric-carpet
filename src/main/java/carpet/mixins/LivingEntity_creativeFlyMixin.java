package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_creativeFlyMixin extends Entity
{
    @Shadow protected abstract float getFlyingSpeed();

    public LivingEntity_creativeFlyMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    @ModifyConstant(method = "travelInAir", constant = @Constant(floatValue = 0.91F), expect = 1)
    private float dragAir(float original)
    {
        if (CarpetSettings.creativeFlyDrag != 0.09 && (Object)this instanceof Player)
        {
            Player self = (Player)(Object)(this);
            if (self.getAbilities().flying && ! onGround() )
                return (float)(1.0-CarpetSettings.creativeFlyDrag);
        }
        return original;
    }

    @Inject(method = "getFrictionInfluencedSpeed(F)F", at = @At("HEAD"), cancellable = true)
    private void flyingAltSpeed(float slipperiness, CallbackInfoReturnable<Float> cir)
    {
        if (CarpetSettings.creativeFlySpeed != 1.0D && (Object)this instanceof Player)
        {
            Player self = (Player)(Object)(this);
            if (self.getAbilities().flying && !onGround())
                cir.setReturnValue( getFlyingSpeed() * (float)CarpetSettings.creativeFlySpeed);
        }
    }

    @Inject(method = "canUsePortal", at = @At("HEAD"), cancellable = true)
    private void canChangeDimensions(CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.isCreativeFlying(this)) {
                cir.setReturnValue(false);
        }
    }
}
