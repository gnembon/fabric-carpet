package carpet.helpers;

import carpet.script.utils.ParticleParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleDisplay
{
    public static void drawParticleLine(ServerPlayer player, Vec3 from, Vec3 to, String main, String accent, int count, double spread)
    {
        ParticleOptions accentParticle = ParticleParser.getEffect(accent, player.server.registryAccess());
        ParticleOptions mainParticle = ParticleParser.getEffect(main, player.server.registryAccess());

        if (accentParticle != null) player.serverLevel().sendParticles(
                player,
                accentParticle,
                true, true,
                to.x, to.y, to.z, count,
                spread, spread, spread, 0.0);

        double lineLengthSq = from.distanceToSqr(to);
        if (lineLengthSq == 0) return;

        Vec3 incvec = to.subtract(from).normalize();//    multiply(50/sqrt(lineLengthSq));
        for (Vec3 delta = new Vec3(0.0,0.0,0.0);
             delta.lengthSqr() < lineLengthSq;
             delta = delta.add(incvec.scale(player.level().random.nextFloat())))
        {
            player.serverLevel().sendParticles(
                    player,
                    mainParticle,
                    true, true,
                    delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                    0.0, 0.0, 0.0, 0.0);
        }
    }

}
