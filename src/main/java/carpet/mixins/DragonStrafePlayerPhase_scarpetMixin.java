package carpet.mixins;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DragonStrafePlayerPhase.class)
public interface DragonStrafePlayerPhase_scarpetMixin {
    @Accessor
    Path getCurrentPath();
}
