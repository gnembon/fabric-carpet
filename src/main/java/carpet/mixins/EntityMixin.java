package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityInterface
{
    @Shadow
    public float yRot;
    
    @Shadow
    public float yRotO;

    @Shadow public @Nullable abstract LivingEntity getControllingPassenger();

    @Shadow public Level level;

    @Override
    public float getMainYaw(float partialTicks)
    {
        return partialTicks == 1.0F ? this.yRot : Mth.lerp(partialTicks, this.yRotO, this.yRot);
    }

    @Inject(method = "isControlledByLocalInstance", at = @At("HEAD"), cancellable = true)
    private void isFakePlayer(CallbackInfoReturnable<Boolean> cir)
    {
        if (getControllingPassenger() instanceof EntityPlayerMPFake) cir.setReturnValue(!level.isClientSide);
    }
}
