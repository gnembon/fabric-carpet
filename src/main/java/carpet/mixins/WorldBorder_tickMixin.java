package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.TickSyncedBorderExtent;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorder.class)
public class WorldBorder_tickMixin
{
    @Shadow private WorldBorder.BorderExtent extent;

    @Inject(method = "lerpSizeBetween", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;getListeners()Ljava/util/List;"))
    private void getExtent(double d, double e, long l, CallbackInfo ci)
    {
        if (d != e && CarpetSettings.tickSyncedWorldBorders)
        {
            this.extent = new TickSyncedBorderExtent((WorldBorder) (Object) this, l, d, e);
        }
    }
}
