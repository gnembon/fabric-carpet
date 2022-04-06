package carpet.utils;

import carpet.CarpetSettings;
import net.minecraft.util.RandomSource;

public class RandomTools
{
    public static double nextGauBian(RandomSource random)
    {
        if (CarpetSettings.extremeBehaviours)
        {
            random.nextDouble();
            return 16.0D * random.nextDouble() - 8.0D;
        }
        return random.nextGaussian();
    }
}
