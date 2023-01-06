package carpet.mixins;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Objective.class)
public interface Objective_scarpetMixin {
    @Mutable @Accessor("criteria")
    void setCriterion(ObjectiveCriteria criterion);
}
