package carpet.logging.logHelpers;

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
    private static ParticleEffect failedPath;
    private static ParticleEffect successfulPath;
    private static ParticleEffect lvl1;
    private static ParticleEffect lvl2;
    private static ParticleEffect lvl3;

    static
    {
        failedPath = parseParticle("angry_villager");
        successfulPath = parseParticle("happy_villager");
        lvl1 = parseParticle("dust 1 1 0 1");
        lvl2 = parseParticle("dust 1 0.5 0 1");
        lvl3 = parseParticle("dust 1 0 0 1");
    }

    private static void drawParticleLine(ServerPlayerEntity player, Vec3d from, Vec3d to, float ratio, boolean successful)
    {
        ParticleEffect accent = successful ? successfulPath : failedPath;
        ParticleEffect color = (ratio < 2)? lvl1 : ((ratio < 4)?lvl2:lvl3);

        ((ServerWorld)player.world).spawnParticles(
                player,
                accent,
                true,
                from.x, from.y, from.z, 5,
                0.5, 0.5, 0.5, 0.0);

        double lineLengthSq = from.squaredDistanceTo(to);
        if (lineLengthSq == 0) return;

        Vec3d incvec = to.subtract(from).normalize();//    multiply(50/sqrt(lineLengthSq));
        int pcount = 0;
        for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
             delta.lengthSquared()<lineLengthSq;
             delta = delta.add(incvec.multiply(player.world.random.nextFloat())))
        {
            ((ServerWorld)player.world).spawnParticles(
                    player,
                    color,
                    true,
                    delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                    0.0, 0.0, 0.0, 0.0);
        }
    }

    private static ParticleEffect parseParticle(String name)
    {
        try
        {
            return ParticleArgumentType.readParameters(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new RuntimeException("No such particle: "+name);
        }
    }


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
            drawParticleLine((ServerPlayerEntity) player, entity.getPos(), target, miliseconds/minDuration, successful );
            return null;
        });
    }
}
