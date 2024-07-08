package carpet.script.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ParticleParser
{
    private static final Map<String, ParticleOptions> particleCache = new HashMap<>(); // we reset this on reloads, but probably need something better

    private static ParticleOptions parseParticle(String name, RegistryAccess lookup)
    {
        try
        {
            return ParticleArgument.readParticle(new StringReader(name), lookup);
        }
        catch (CommandSyntaxException e)
        {
            throw new IllegalArgumentException("No such particle: " + name);
        }
    }

    @Nullable
    public static ParticleOptions getEffect(@Nullable String name, RegistryAccess lookup)
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
