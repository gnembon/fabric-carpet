package carpet.mixins;

import carpet.fakes.EntityInterface;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityInterface
{
    @Shadow
    public float yaw;
    
    @Shadow
    public float prevYaw;
    
    public float getMainYaw(float partialTicks)
    {
        return partialTicks == 1.0F ? this.yaw : MathHelper.lerp(partialTicks, this.prevYaw, this.yaw);
    }
}
