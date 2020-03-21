package carpet.utils;

import carpet.CarpetSettings;

import java.util.Random;

public class RandomTools
{
    public static double nextGauBian(Random random)
    {
        if (CarpetSettings.extremeBehaviours)
        {
            random.nextDouble();
            return 16.0D * random.nextDouble() - 8.0D;
        }
        return random.nextGaussian();
    }
}
