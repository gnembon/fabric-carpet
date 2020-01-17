package carpet.mixins;

import net.minecraft.village.PointOfInterest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PointOfInterest.class)
public interface PointOfInterest_scarpetMixin
{
    @Accessor("freeTickets")
    int getFreeTickets();
}
