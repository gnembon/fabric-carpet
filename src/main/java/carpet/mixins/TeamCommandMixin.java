package carpet.mixins;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeamCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TeamCommand.class)
public class TeamCommandMixin {
    @Inject(method = "executeRemove", at = @At("HEAD"))
    private static void preventRemovingBotTeam(ServerCommandSource source, Team team, CallbackInfoReturnable<Integer> cir) {
        Scoreboard scoreboard = source.getMinecraftServer().getScoreboard();
        if (scoreboard.getTeam("fake_players").equals(team)) {
            cir.setReturnValue(scoreboard.getTeams().size());
            cir.cancel();
        }
    }
}
