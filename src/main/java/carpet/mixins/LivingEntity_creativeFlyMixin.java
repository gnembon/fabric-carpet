package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
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
    @Shadow public float flyingSpeed;

    public LivingEntity_creativeFlyMixin(EntityType<?> type, World world)
    {
        super(type, world);
    }

    @ModifyConstant(method = "travel", constant = @Constant(floatValue = 0.91F), expect = 2)
    private float drag(float original)
    {
        if (CarpetSettings.creativeFlyDrag != 0.09 && (Object)this instanceof PlayerEntity)
        {
            PlayerEntity self = (PlayerEntity)(Object)(this);
            if (self.abilities.flying && ! onGround )
                return (float)(1.0-CarpetSettings.creativeFlyDrag);
        }
        return original;
    }


    @Inject(method = "getMovementSpeed(F)F", at = @At("HEAD"), cancellable = true)
    private void flyingAltSpeed(float slipperiness, CallbackInfoReturnable<Float> cir)
    {
        if (CarpetSettings.creativeFlySpeed != 1.0D && (Object)this instanceof PlayerEntity)
        {
            PlayerEntity self = (PlayerEntity)(Object)(this);
            if (self.abilities.flying && !onGround)
                cir.setReturnValue(flyingSpeed* (float)CarpetSettings.creativeFlySpeed);
        }
    }
}
