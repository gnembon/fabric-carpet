package carpet.mixins;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DragonHoldingPatternPhase.class)
public interface DragonHoldingPatternPhase_scarpetMixin {
    @Accessor
    Path getCurrentPath();
}
