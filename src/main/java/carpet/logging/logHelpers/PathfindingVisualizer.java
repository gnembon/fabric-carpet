package carpet.logging.logHelpers;

import carpet.helpers.ParticleDisplay;
import carpet.logging.LoggerRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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

            ParticleOptions accent = successful ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
            ParticleOptions color = (miliseconds/minDuration < 2)? new DustParticleOptions(0xffffff00, 1) : ((miliseconds/minDuration < 4)?new DustParticleOptions(0xFFFF7700, 1): new DustParticleOptions(0xFFFF0000, 1));
            ParticleDisplay.drawParticleLine((ServerPlayer) player, entity.position(), target, color, accent, 5, 0.5);
            return null;
        });
    }
}
