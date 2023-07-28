package carpet.script.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Random;

// extracted from import net.minecraft.util.math.noise.PerlinNoiseSampler
public class PerlinNoiseSampler
{
    protected static final int[][] gradients3d = new int[][]{
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
            {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
    };
    protected static final int[][] gradients2d = new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};


    private final byte[] permutations;
    public final double originX;
    public final double originY;
    public final double originZ;
    public static PerlinNoiseSampler instance = new PerlinNoiseSampler(new Random(0));
    public static Map<Long, PerlinNoiseSampler> samplers = new Long2ObjectOpenHashMap<>();

    public static PerlinNoiseSampler getPerlin(long aLong)
    {
        if (samplers.size() > 256)
        {
            samplers.clear();
        }
        return samplers.computeIfAbsent(aLong, seed -> new PerlinNoiseSampler(new Random(seed)));
    }

    public PerlinNoiseSampler(Random random)
    {
        this.originX = random.nextDouble() * 256.0D;
        this.originY = random.nextDouble() * 256.0D;
        this.originZ = random.nextDouble() * 256.0D;
        this.permutations = new byte[256];

        int j;
        for (j = 0; j < 256; ++j)
        {
            this.permutations[j] = (byte) j;
        }
        for (j = 0; j < 256; ++j)
        {
            int k = random.nextInt(256 - j);
            byte b = this.permutations[j];
            this.permutations[j] = this.permutations[j + k];
            this.permutations[j + k] = b;
        }
    }
    //3D
    public double sample3d(double x, double y, double z)
    {//, double d, double e) {
        double f = x + this.originX;
        double g = y + this.originY;
        double h = z + this.originZ;
        int i = Mth.floor(f);
        int j = Mth.floor(g);
        int k = Mth.floor(h);
        double l = f - (double) i;
        double m = g - (double) j;
        double n = h - (double) k;
        double o = perlinFade(l);
        double p = perlinFade(m);
        double q = perlinFade(n);
        //double t;
        /*
        if (d != 0.0D) {
            double r = Math.min(e, m);
            t = (double)Mth.floor(r / d) * d;
        } else {
            t = 0.0D;
        }*/
        //return this.sample(i, j, k, l, m - t, n, o, p, q);
        return this.sample3d(i, j, k, l, m, n, o, p, q) / 2 + 0.5;
    }

    private double sample3d(int sectionX, int sectionY, int sectionZ, double localX, double localY, double localZ, double fadeLocalX, double fadeLocalY, double fadeLocalZ)
    {
        int i = this.getGradient(sectionX) + sectionY;
        int j = this.getGradient(i) + sectionZ;
        int k = this.getGradient(i + 1) + sectionZ;
        int l = this.getGradient(sectionX + 1) + sectionY;
        int m = this.getGradient(l) + sectionZ;
        int n = this.getGradient(l + 1) + sectionZ;
        double d = grad3d(this.getGradient(j), localX, localY, localZ);
        double e = grad3d(this.getGradient(m), localX - 1.0D, localY, localZ);
        double f = grad3d(this.getGradient(k), localX, localY - 1.0D, localZ);
        double g = grad3d(this.getGradient(n), localX - 1.0D, localY - 1.0D, localZ);
        double h = grad3d(this.getGradient(j + 1), localX, localY, localZ - 1.0D);
        double o = grad3d(this.getGradient(m + 1), localX - 1.0D, localY, localZ - 1.0D);
        double p = grad3d(this.getGradient(k + 1), localX, localY - 1.0D, localZ - 1.0D);
        double q = grad3d(this.getGradient(n + 1), localX - 1.0D, localY - 1.0D, localZ - 1.0D);
        return lerp3(fadeLocalX, fadeLocalY, fadeLocalZ, d, e, f, g, h, o, p, q);
    }

    private static double grad3d(int hash, double x, double y, double z)
    {
        int i = hash & 15;
        return dot3d(gradients3d[i], x, y, z);
    }

    protected static double dot3d(int[] gArr, double x, double y, double z)
    {
        return gArr[0] * x + gArr[1] * y + gArr[2] * z;
    }

    public static double lerp3(double deltaX, double deltaY, double deltaZ, double d, double e, double f, double g, double h, double i, double j, double k)
    {
        return lerp(deltaZ, lerp2(deltaX, deltaY, d, e, f, g), lerp2(deltaX, deltaY, h, i, j, k));
    }

    //2D
    public double sample2d(double x, double y)
    {
        double f = x + this.originX;
        double g = y + this.originY;
        int i = Mth.floor(f);
        int j = Mth.floor(g);
        double l = f - (double) i;
        double m = g - (double) j;
        double o = perlinFade(l);
        double p = perlinFade(m);
        return this.sample2d(i, j, l, m, o, p) / 2 + 0.5;
    }

    private double sample2d(int sectionX, int sectionY, double localX, double localY, double fadeLocalX, double fadeLocalY)
    {
        int j = this.getGradient(sectionX) + sectionY;
        int m = this.getGradient(sectionX + 1) + sectionY;
        double d = grad2d(this.getGradient(j), localX, localY);
        double e = grad2d(this.getGradient(m), localX - 1.0D, localY);
        double f = grad2d(this.getGradient(j + 1), localX, localY - 1.0D);
        double g = grad2d(this.getGradient(m + 1), localX - 1.0D, localY - 1.0D);

        return lerp2(fadeLocalX, fadeLocalY, d, e, f, g);
    }

    private static double grad2d(int hash, double x, double y)
    {
        int i = hash & 3;
        return dot2d(gradients2d[i], x, y);
    }

    protected static double dot2d(int[] gArr, double x, double y)
    {
        return gArr[0] * x + gArr[1] * y;
    }

    public static double lerp2(double deltaX, double deltaY, double d, double e, double f, double g)
    {
        return lerp(deltaY, lerp(deltaX, d, e), lerp(deltaX, f, g));
    }

    // 1D
    public double sample1d(double x)
    {
        double f = x + this.originX;
        int i = Mth.floor(f);
        double l = f - i;
        double o = perlinFade(l);
        return this.sample1d(i, l, o) + 0.5;
    }

    private double sample1d(int sectionX, double localX, double fadeLocalX)
    {
        double d = grad1d(this.getGradient(sectionX), localX);
        double e = grad1d(this.getGradient(sectionX + 1), localX - 1.0D);
        return lerp(fadeLocalX, d, e);
    }

    private static double grad1d(int hash, double x)
    {
        return ((hash & 1) == 0) ? x : -x;
    }

    public static double lerp(double delta, double first, double second)
    {
        return first + delta * (second - first);
    }

    // shared
    public int getGradient(int hash)
    {
        return this.permutations[hash & 255] & 255;
    }

    public static double perlinFade(double d)
    {
        return d * d * d * (d * (d * 6.0D - 15.0D) + 10.0D);
    }
}
