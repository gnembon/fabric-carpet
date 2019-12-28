package carpet.mixins;

import carpet.fakes.TicketInfoInterface;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.PortalForcer$TicketInfo")
public class PortalForcerTicketInfo_portalCachingMixin implements TicketInfoInterface
{
    @Shadow @Final public BlockPos pos;

    @Override
    public BlockPos getPos()
    {
        return pos;
    }
}
