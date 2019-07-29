package carpet.helpers;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ParticleArgumentType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class ParticleDisplay
{
    private static Map<String, ParticleEffect> particleCache = new HashMap<>();

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
    public static ParticleEffect getEffect(String name)
    {
        if (name == null) return null;
        ParticleEffect res = particleCache.get(name);
        if (res != null) return res;
        particleCache.put(name, parseParticle(name));
        return particleCache.get(name);
    }

    public static void drawParticleLine(ServerPlayerEntity player, Vec3d from, Vec3d to,String main)
    {
        drawParticleLine(player, from, to, main, null, 1, 0.0);
    }

    public static void drawParticleLine(ServerPlayerEntity player, Vec3d from, Vec3d to, String main, String accent, int count, double spread)
    {
        ParticleEffect accentParticle = getEffect(accent);
        ParticleEffect mainParticle = getEffect(main);

        if (accentParticle != null) ((ServerWorld)player.world).spawnParticles(
                player,
                accentParticle,
                true,
                to.x, to.y, to.z, count,
                spread, spread, spread, 0.0);

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
                    mainParticle,
                    true,
                    delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                    0.0, 0.0, 0.0, 0.0);
        }
    }

}
