package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.EntityInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntity_scarpetEventsMixin
{
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCall(CallbackInfo ci)
    {
        if (((EntityInterface)this).getTickCallback() != null)
        {
            CarpetServer.scriptServer.events.onEntityTick(((EntityInterface)this).getTickCallback(), (Entity) (Object) this);
        }
    }
}
