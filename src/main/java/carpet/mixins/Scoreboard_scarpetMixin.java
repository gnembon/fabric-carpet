package carpet.mixins;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(Scoreboard.class)
public interface Scoreboard_scarpetMixin {
    @Accessor("objectivesByCriterion")
    Map<ScoreboardCriterion, List<ScoreboardObjective>> getObjectivesByCriterion();
}
