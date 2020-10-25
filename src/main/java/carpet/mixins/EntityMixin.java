package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
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
    public float yaw;
    
    @Shadow
    public float prevYaw;

    @Shadow public @Nullable abstract Entity getPrimaryPassenger();

    @Shadow public World world;

    public float getMainYaw(float partialTicks)
    {
        return partialTicks == 1.0F ? this.yaw : MathHelper.lerp(partialTicks, this.prevYaw, this.yaw);
    }

    @Inject(method = "isLogicalSideForUpdatingMovement", at = @At("HEAD"), cancellable = true)
    private void isFakePlayer(CallbackInfoReturnable<Boolean> cir)
    {
        if (getPrimaryPassenger() instanceof EntityPlayerMPFake) cir.setReturnValue(!world.isClient);
    }
}
