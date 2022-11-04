package carpet.mixins;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingApproachPhase;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DragonLandingApproachPhase.class)
public interface DragonLandingApproachPhase_scarpetMixin {
    @Accessor
    Path getCurrentPath();
}
