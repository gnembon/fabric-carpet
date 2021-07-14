package carpet.mixins;

import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScoreboardObjective.class)
public interface ScoreboardObjective_scarpetMixin {
    @Mutable @Accessor("criterion")
    void setCriterion(ScoreboardCriterion criterion);
}
