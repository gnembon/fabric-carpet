package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = PistonStructureResolver.class, priority = 420)  // piston push limit is important for carpet
public class PistonStructureResolver_pushLimitMixin
{
    @ModifyConstant(method = "addBlockLine", constant = @Constant(intValue = PistonStructureResolver.MAX_PUSH_DEPTH), expect = 3)
    private int pushLimit(int original)
    {
        return CarpetSettings.pushLimit;
    }
}
