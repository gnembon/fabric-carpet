package carpet.logging.logHelpers;

import carpet.helpers.ParticleDisplay;
import carpet.logging.LoggerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PathfindingVisualizer
{
    public static void slowPath(Entity entity, Vec3 target, float miliseconds, boolean successful)
    {
        if (!LoggerRegistry.__pathfinding) return;
        LoggerRegistry.getLogger("pathfinding").log( (option, player)->
        {
            if (!(player instanceof ServerPlayer))
                return null;
            int minDuration;
            try
            {
                minDuration = Integer.parseInt(option);
            }
            catch (NumberFormatException ignored)
            {
                return  null;
            }
            if (miliseconds < minDuration)
                return null;
            if (player.distanceToSqr(entity) > 1000 && player.distanceToSqr(target) > 1000)
                return null;
            if (minDuration < 1)
                minDuration = 1;

            String accent = successful ? "happy_villager" : "angry_villager";
            String color = (miliseconds/minDuration < 2)? "dust{color:[1,1,0],scale:1.0}" : ((miliseconds/minDuration < 4)?"dust{color:[1,0.5,0],scale:1}":"dust{color:[1,0,0],scale:1}");
            ParticleDisplay.drawParticleLine((ServerPlayer) player, entity.position(), target, color, accent, 5, 0.5);
            return null;
        });
    }
}
