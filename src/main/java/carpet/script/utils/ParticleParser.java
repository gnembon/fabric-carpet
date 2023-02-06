package carpet.script.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.HashMap;
import java.util.Map;

public class ParticleParser
{
    private static final Map<String, ParticleOptions> particleCache = new HashMap<>(); // we reset this on reloads, but probably need something better

    private static ParticleOptions parseParticle(final String name, final HolderLookup<ParticleType<?>> lookup)
    {
        try
        {
            return ParticleArgument.readParticle(new StringReader(name), lookup);
        }
        catch (final CommandSyntaxException e)
        {
            throw new IllegalArgumentException("No such particle: " + name);
        }
    }

    public static ParticleOptions getEffect(final String name, final HolderLookup<ParticleType<?>> lookup)
    {
        if (name == null)
        {
            return null;
        }
        return particleCache.computeIfAbsent(name, particle -> parseParticle(particle, lookup));
    }

    public static void resetCache()
    {
        particleCache.clear();
    }
}
