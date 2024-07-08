package carpet.mixins;

import carpet.fakes.PortalProcessorInterface;
import net.minecraft.world.entity.PortalProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PortalProcessor.class)
public class PortalProcessor_scarpetMixin implements PortalProcessorInterface
{

    @Shadow private int portalTime;

    @Override
    public void setPortalTime(int time)
    {
        portalTime = time;
    }
}
