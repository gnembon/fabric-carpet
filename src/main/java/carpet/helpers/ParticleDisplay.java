package carpet.helpers;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleDisplay
{
    private static Map<String, ParticleOptions> particleCache = new HashMap<>();

    private static ParticleOptions parseParticle(String name, ServerLevel level)  // [SCARY SHIT] persistent caches over server reloads
    {
        try
        {
            return ParticleArgument.readParticle(new StringReader(name), level.holderLookup(Registries.PARTICLE_TYPE));
        }
        catch (CommandSyntaxException e)
        {
            throw new RuntimeException("No such particle: "+name);
        }
    }
    public static ParticleOptions getEffect(String name, ServerLevel level)
    {
        if (name == null) return null;
        ParticleOptions res = particleCache.get(name);
        if (res != null) return res;
        particleCache.put(name, parseParticle(name, level));
        return particleCache.get(name);
    }

    public static void drawParticleLine(ServerPlayer player, Vec3 from, Vec3 to, String main, String accent, int count, double spread)
    {
        ParticleOptions accentParticle = getEffect(accent, player.getLevel());
        ParticleOptions mainParticle = getEffect(main, player.getLevel());

        if (accentParticle != null) ((ServerLevel)player.level).sendParticles(
                player,
                accentParticle,
                true,
                to.x, to.y, to.z, count,
                spread, spread, spread, 0.0);

        double lineLengthSq = from.distanceToSqr(to);
        if (lineLengthSq == 0) return;

        Vec3 incvec = to.subtract(from).normalize();//    multiply(50/sqrt(lineLengthSq));
        for (Vec3 delta = new Vec3(0.0,0.0,0.0);
             delta.lengthSqr()<lineLengthSq;
             delta = delta.add(incvec.scale(player.level.random.nextFloat())))
        {
            ((ServerLevel)player.level).sendParticles(
                    player,
                    mainParticle,
                    true,
                    delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                    0.0, 0.0, 0.0, 0.0);
        }
    }

}
