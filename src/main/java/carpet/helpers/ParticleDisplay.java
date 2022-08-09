package carpet.helpers;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleDisplay
{
    private static final Map<String, ParticleOptions> particleCache = new HashMap<>();

    private static ParticleOptions parseParticle(String name)
    {
        try
        {
            return ParticleArgument.readParticle(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new IllegalArgumentException("No such particle: " + name);
        }
    }
    public static ParticleOptions getEffect(String name)
    {
        if (name == null) return null;
        return particleCache.computeIfAbsent(name, ParticleDisplay::parseParticle);
    }

    public static void drawParticleLine(ServerPlayer player, Vec3 from, Vec3 to, String main, String accent, int count, double spread)
    {
        ParticleOptions accentParticle = getEffect(accent);
        ParticleOptions mainParticle = getEffect(main);

        if (accentParticle != null) player.getLevel().sendParticles(
                player,
                accentParticle,
                true,
                to.x, to.y, to.z, count,
                spread, spread, spread, 0.0);

        double lineLengthSq = from.distanceToSqr(to);
        if (lineLengthSq == 0) return;

        Vec3 incvec = to.subtract(from).normalize();//    multiply(50/sqrt(lineLengthSq));
        for (Vec3 delta = new Vec3(0.0,0.0,0.0);
             delta.lengthSqr() < lineLengthSq;
             delta = delta.add(incvec.scale(player.level.random.nextFloat())))
        {
            player.getLevel().sendParticles(
                    player,
                    mainParticle,
                    true,
                    delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                    0.0, 0.0, 0.0, 0.0);
        }
    }

}
