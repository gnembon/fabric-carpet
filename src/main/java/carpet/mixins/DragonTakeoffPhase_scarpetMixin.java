package carpet.mixins;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DragonTakeoffPhase.class)
public interface DragonTakeoffPhase_scarpetMixin {
    @Accessor
    Path getCurrentPath();
}
