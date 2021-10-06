package carpet.mixins;

import carpet.CarpetSettings;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityInterface
{
    private static final AtomicInteger SERVER_CURRENT_ID = new AtomicInteger();

    @Shadow
    public float yaw;
    
    @Shadow
    public float prevYaw;

    @Shadow public @Nullable abstract Entity getPrimaryPassenger();

    @Shadow public World world;

    @Shadow public abstract void setId(int id);

    @Inject(at=@At("RETURN"), method="<init>")
    private void constructor(CallbackInfo info) {
        if(CarpetSettings.localServerEntityIdFix) {
            if (!world.isClient()) {
                setId(SERVER_CURRENT_ID.incrementAndGet());
            }
        } else {
            SERVER_CURRENT_ID.incrementAndGet();
        }
    }

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
