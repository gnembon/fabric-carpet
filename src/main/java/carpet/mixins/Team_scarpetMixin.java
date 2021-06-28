package carpet.mixins;

import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Team.class)
public interface Team_scarpetMixin
{
    @Accessor("color")
    Formatting getColor();
}
