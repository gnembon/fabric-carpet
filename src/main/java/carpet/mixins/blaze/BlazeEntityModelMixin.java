package carpet.mixins.blaze;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BlazeEntityModel;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeEntityModel.class)
public class BlazeEntityModelMixin<T extends Entity>
{
    @Shadow @Final private ModelPart head;

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void setAnimations(T entity, float limbAngle, float limbDistance, float customAngle, float headYaw, float headPitch, CallbackInfo ci)
    {
        head.yaw = headYaw * 0.017453292F;
        head.pitch = headPitch * 0.017453292F;
        ci.cancel();
    }

}
