package carpet.logging.logHelpers;

import carpet.helpers.ParticleDisplay;
import carpet.logging.LoggerRegistry;
import carpet.logging.TypeLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PathfindingVisualizer
{
    @SuppressWarnings("unchecked")
    public static void slowPath(Entity entity, Vec3 target, float miliseconds, boolean successful)
    {
        if (!LoggerRegistry.__pathfinding) return;
        ((TypeLogger<Integer>)LoggerRegistry.getLogger("pathfinding")).log((TypeLogger.TypeMessage<Integer>) (option, player)->
        {
            if (!(player instanceof ServerPlayer))
                return null;
            if (miliseconds < option)
                return null;
            if (player.distanceToSqr(entity) > 1000 && player.distanceToSqr(target) > 1000)
                return null;
            if (option < 1)
                option = 1;
            String accent = successful ? "happy_villager" : "angry_villager";
            String color = (miliseconds/option < 2)? "dust 1 1 0 1" : ((miliseconds/option < 4)?"dust 1 0.5 0 1":"dust 1 0 0 1");
            ParticleDisplay.drawParticleLine((ServerPlayer) player, entity.position(), target, color, accent, 5, 0.5);
            return null;
        });
    }
}
