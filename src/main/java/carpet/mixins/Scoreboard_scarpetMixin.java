package carpet.mixins;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

@Mixin(Scoreboard.class)
public interface Scoreboard_scarpetMixin {
    @Accessor("objectivesByCriteria")
    Reference2ObjectMap<ObjectiveCriteria, List<Objective>> getObjectivesByCriterion();
}
