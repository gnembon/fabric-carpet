package carpet.logging.logHelpers;

import carpet.helpers.ParticleDisplay;
import carpet.logging.LoggerRegistry;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ParticleArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class PathfindingVisualizer
{
    public static void slowPath(Entity entity, Vec3d target, float miliseconds, boolean successful)
    {
        if (!LoggerRegistry.__pathfinding) return;
        LoggerRegistry.getLogger("pathfinding").log( (option, player)->
        {
            if (!(player instanceof ServerPlayerEntity))
                return null;
            int minDuration = Integer.parseInt(option);
            if (miliseconds < minDuration)
                return null;
            if (player.squaredDistanceTo(entity) > 1000 && player.squaredDistanceTo(target) > 1000)
                return null;
            if (minDuration < 1)
                minDuration = 1;

            String accent = successful ? "happy_villager" : "angry_villager";
            String color = (miliseconds/minDuration < 2)? "dust 1 1 0 1" : ((miliseconds/minDuration < 4)?"dust 1 0.5 0 1":"dust 1 0 0 1");
            ParticleDisplay.drawParticleLine((ServerPlayerEntity) player, entity.getPos(), target, color, accent, 5, 0.5);
            return null;
        });
    }
}
