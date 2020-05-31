package carpet.script.utils;

import carpet.network.ServerNetworkHandler;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ParticleArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Math.sqrt;

public class ShapeDispatcher
{
    private static final Map<String, ParticleEffect> particleCache = new HashMap<>();

    public static void sendShape(List<ServerPlayerEntity> players, RegistryKey<World> dim, ExpiringShape shape)
    {
        Consumer<ServerPlayerEntity> alternative = null;
        CompoundTag tag = null;
        for (ServerPlayerEntity player : players)
        {
            if (ServerNetworkHandler.validCarpetPlayers.contains(player))
            {
                if (tag == null) tag = shape.toTag();
                tag.putString("dim", dim.getValue().toString());
                ServerNetworkHandler.sendCustomCommand(player,"renderShape", tag);
            }
            else
            {
                if (alternative == null) alternative = shape.alternative();
                alternative.accept(player);
            }
        }
    }

    public static void sendBox(ServerPlayerEntity player, int duration, Vec3d from, Vec3d to, int color)
    {

    }

    public static ParticleEffect getParticleData(String name)
    {
        ParticleEffect particle = particleCache.get(name);
        if (particle != null)
            return particle;
        try
        {
            particle = ParticleArgumentType.readParameters(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("No such particle: "+name);
        }
        particleCache.put(name, particle);
        return particle;
    }


    public abstract static class ExpiringShape
    {
        protected float r, g, b, a;
        protected int color;
        protected int duration = 0;
        public ExpiringShape(int duration, int color)
        {
            this.r = (float)(color >> 24 & 0xFF) / 255.0F;
            this.g = (float)(color >> 16 & 0xFF) / 255.0F;
            this.b = (float)(color >>  8 & 0xFF) / 255.0F;
            this.a = (float)(color & 0xFF) / 255.0F;
            this.color = color;
            this.duration = duration;
        };

        public int getExpiry() { return duration; };
        public CompoundTag toTag()
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt("color", color);
            tag.putInt("duration", duration);
            return tag;
        };
        public abstract Consumer<ServerPlayerEntity> alternative();
    }

    public static class Box extends ExpiringShape
    {
        float x1, y1, z1;
        float x2, y2, z2;
        public Box(int duration, Vec3d from, Vec3d to, int color)
        {
            super(duration, color);
            x1 = (float)from.x;
            y1 = (float)from.y;
            z1 = (float)from.z;
            x2 = (float)to.x;
            y2 = (float)to.y;
            z2 = (float)to.z;
        }


        public static ExpiringShape fromTag(CompoundTag tag)
        {
            return new Box(tag.getInt("duration"),
                    new Vec3d(tag.getFloat("x1"), tag.getFloat("y1"), tag.getFloat("z1")),
                    new Vec3d(tag.getFloat("x2"), tag.getFloat("y2"), tag.getFloat("z2")),
                    tag.getInt("color")
            );
        }

        @Override
        public CompoundTag toTag()
        {
            CompoundTag tag = super.toTag();
            tag.putFloat("x1", x1);
            tag.putFloat("y1", y1);
            tag.putFloat("z1", z1);
            tag.putFloat("x2", x2);
            tag.putFloat("y2", y2);
            tag.putFloat("z2", z2);
            tag.putString("type", "debugBox");
            return tag;
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            double dx = x1-x2;
            double dy = y1-y2;
            double dz = z1-z2;
            double density = Math.max(2.0, Math.sqrt(dx*dx+dy*dy+dz*dz) /50) / (a+0.1);
            return p -> mesh(Collections.singletonList(p), particle, density, x1, y1, z1, x2, y2, z2);
        }

        public static int mesh(List<ServerPlayerEntity> playerList, ParticleEffect particle, double density,
                               double x1, double y1, double z1, double x2, double y2, double z2 )
        {
            return
            drawParticleLine(playerList, particle, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), density)+
            drawParticleLine(playerList, particle, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z1), density)+

            drawParticleLine(playerList, particle, new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), density)+

            drawParticleLine(playerList, particle, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), density);
        }
    }

    public static class Line extends ExpiringShape
    {
        float x1, y1, z1;
        float x2, y2, z2;
        public Line(int duration, Vec3d from, Vec3d to, int color)
        {
            super(duration, color);
            x1 = (float)from.x;
            y1 = (float)from.y;
            z1 = (float)from.z;
            x2 = (float)to.x;
            y2 = (float)to.y;
            z2 = (float)to.z;

        }
        @Override
        public CompoundTag toTag()
        {
            CompoundTag tag = super.toTag();
            tag.putFloat("x1", x1);
            tag.putFloat("y1", y1);
            tag.putFloat("z1", z1);
            tag.putFloat("x2", x2);
            tag.putFloat("y2", y2);
            tag.putFloat("z2", z2);
            tag.putString("type", "debugLine");
            return tag;
        }

        public static ExpiringShape fromTag(CompoundTag tag)
        {
            return new Line(tag.getInt("duration"),
                    new Vec3d(tag.getFloat("x1"), tag.getFloat("y1"), tag.getFloat("z1")),
                    new Vec3d(tag.getFloat("x2"), tag.getFloat("y2"), tag.getFloat("z2")),
                    tag.getInt("color")
            );
        }
        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            Vec3d pos1 = new Vec3d(x1, y1, z1);
            Vec3d pos2 = new Vec3d(x2, y2, z2);
            double dx = x1-x2;
            double dy = y1-y2;
            double dz = z1-z2;
            double density = Math.max(2.0, Math.sqrt(dx*dx+dy*dy+dz*dz) /50) / (a+0.1);
            return p -> drawParticleLine(Collections.singletonList(p), particle, pos1, pos2, density);
        }
    }

    private static boolean isStraight(Vec3d from, Vec3d to, double density)
    {
        if ( (from.x == to.x && from.y == to.y) || (from.x == to.x && from.z == to.z) || (from.y == to.y && from.z == to.z))
            return from.distanceTo(to) / density > 20;
        return false;
    }

    private static int drawOptimizedParticleLine(List<ServerPlayerEntity> playerList, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        double distance = from.distanceTo(to);
        int particles = (int)(distance/density);
        Vec3d towards = to.subtract(from);
        int parts = 0;
        for (ServerPlayerEntity player : playerList)
        {
            ServerWorld world = player.getServerWorld();
            world.spawnParticles(player, particle, true,
                    (towards.x)/2+from.x, (towards.y)/2+from.y, (towards.z)/2+from.z, particles/3,
                    towards.x/6, towards.y/6, towards.z/6, 0.0);
            world.spawnParticles(player, particle, true,
                    from.x, from.y, from.z,1,0.0,0.0,0.0,0.0);
            world.spawnParticles(player, particle, true,
                    to.x, to.y, to.z,1,0.0,0.0,0.0,0.0);
            parts += particles/3+2;
        }
        int divider = 6;
        while (particles/divider > 1)
        {
            int center = (divider*2)/3;
            int dev = 2*divider;
            for (ServerPlayerEntity player : playerList)
            {
                ServerWorld world = player.getServerWorld();
                world.spawnParticles(player, particle, true,
                        (towards.x)/center+from.x, (towards.y)/center+from.y, (towards.z)/center+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
                world.spawnParticles(player, particle, true,
                        (towards.x)*(1.0-1.0/center)+from.x, (towards.y)*(1.0-1.0/center)+from.y, (towards.z)*(1.0-1.0/center)+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
            }
            parts += 2*particles/divider;
            divider = 2*divider;
        }
        return parts;
    }

    public static int drawParticleLine(List<ServerPlayerEntity> players, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        if (isStraight(from, to, density)) return drawOptimizedParticleLine(players, particle, from, to, density);
        double lineLengthSq = from.squaredDistanceTo(to);
        if (lineLengthSq == 0) return 0;
        Vec3d incvec = to.subtract(from).multiply(2*density/sqrt(lineLengthSq));
        int pcount = 0;
        for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
             delta.lengthSquared()<lineLengthSq;
             delta = delta.add(incvec.multiply(Expression.randomizer.nextFloat())))
        {
            for (ServerPlayerEntity player : players)
            {
                player.getServerWorld().spawnParticles(player, particle, true,
                        delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                        0.0, 0.0, 0.0, 0.0);
                pcount ++;
            }
        }
        return pcount;
    }
}
