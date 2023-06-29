package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

@Mixin(Scoreboard.class)
public interface Scoreboard_scarpetMixin {
    @Accessor("objectivesByCriteria")
    Map<ObjectiveCriteria, List<Objective>> getObjectivesByCriterion();
}
