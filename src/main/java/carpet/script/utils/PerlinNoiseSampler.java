package carpet.script.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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

    public static PerlinNoiseSampler getPerlin(final long aLong)
    {
        if (samplers.size() > 256)
        {
            samplers.clear();
        }
        return samplers.computeIfAbsent(aLong, seed -> new PerlinNoiseSampler(new Random(seed)));
    }

    public PerlinNoiseSampler(final Random random)
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
            final int k = random.nextInt(256 - j);
            final byte b = this.permutations[j];
            this.permutations[j] = this.permutations[j + k];
            this.permutations[j + k] = b;
        }
    }
    //3D
    public double sample3d(final double x, final double y, final double z)
    {//, double d, double e) {
        final double f = x + this.originX;
        final double g = y + this.originY;
        final double h = z + this.originZ;
        final int i = floor(f);
        final int j = floor(g);
        final int k = floor(h);
        final double l = f - (double) i;
        final double m = g - (double) j;
        final double n = h - (double) k;
        final double o = perlinFade(l);
        final double p = perlinFade(m);
        final double q = perlinFade(n);
        //double t;
        /*
        if (d != 0.0D) {
            double r = Math.min(e, m);
            t = (double)floor(r / d) * d;
        } else {
            t = 0.0D;
        }*/
        //return this.sample(i, j, k, l, m - t, n, o, p, q);
        return this.sample3d(i, j, k, l, m, n, o, p, q) / 2 + 0.5;
    }

    private double sample3d(final int sectionX, final int sectionY, final int sectionZ, final double localX, final double localY, final double localZ, final double fadeLocalX, final double fadeLocalY, final double fadeLocalZ)
    {
        final int i = this.getGradient(sectionX) + sectionY;
        final int j = this.getGradient(i) + sectionZ;
        final int k = this.getGradient(i + 1) + sectionZ;
        final int l = this.getGradient(sectionX + 1) + sectionY;
        final int m = this.getGradient(l) + sectionZ;
        final int n = this.getGradient(l + 1) + sectionZ;
        final double d = grad3d(this.getGradient(j), localX, localY, localZ);
        final double e = grad3d(this.getGradient(m), localX - 1.0D, localY, localZ);
        final double f = grad3d(this.getGradient(k), localX, localY - 1.0D, localZ);
        final double g = grad3d(this.getGradient(n), localX - 1.0D, localY - 1.0D, localZ);
        final double h = grad3d(this.getGradient(j + 1), localX, localY, localZ - 1.0D);
        final double o = grad3d(this.getGradient(m + 1), localX - 1.0D, localY, localZ - 1.0D);
        final double p = grad3d(this.getGradient(k + 1), localX, localY - 1.0D, localZ - 1.0D);
        final double q = grad3d(this.getGradient(n + 1), localX - 1.0D, localY - 1.0D, localZ - 1.0D);
        return lerp3(fadeLocalX, fadeLocalY, fadeLocalZ, d, e, f, g, h, o, p, q);
    }

    private static double grad3d(final int hash, final double x, final double y, final double z)
    {
        final int i = hash & 15;
        return dot3d(gradients3d[i], x, y, z);
    }

    protected static double dot3d(final int[] gArr, final double x, final double y, final double z)
    {
        return gArr[0] * x + gArr[1] * y + gArr[2] * z;
    }

    public static double lerp3(final double deltaX, final double deltaY, final double deltaZ, final double d, final double e, final double f, final double g, final double h, final double i, final double j, final double k)
    {
        return lerp(deltaZ, lerp2(deltaX, deltaY, d, e, f, g), lerp2(deltaX, deltaY, h, i, j, k));
    }

    //2D
    public double sample2d(final double x, final double y)
    {
        final double f = x + this.originX;
        final double g = y + this.originY;
        final int i = floor(f);
        final int j = floor(g);
        final double l = f - (double) i;
        final double m = g - (double) j;
        final double o = perlinFade(l);
        final double p = perlinFade(m);
        return this.sample2d(i, j, l, m, o, p) / 2 + 0.5;
    }

    private double sample2d(final int sectionX, final int sectionY, final double localX, final double localY, final double fadeLocalX, final double fadeLocalY)
    {
        final int j = this.getGradient(sectionX) + sectionY;
        final int m = this.getGradient(sectionX + 1) + sectionY;
        final double d = grad2d(this.getGradient(j), localX, localY);
        final double e = grad2d(this.getGradient(m), localX - 1.0D, localY);
        final double f = grad2d(this.getGradient(j + 1), localX, localY - 1.0D);
        final double g = grad2d(this.getGradient(m + 1), localX - 1.0D, localY - 1.0D);

        return lerp2(fadeLocalX, fadeLocalY, d, e, f, g);
    }

    private static double grad2d(final int hash, final double x, final double y)
    {
        final int i = hash & 3;
        return dot2d(gradients2d[i], x, y);
    }

    protected static double dot2d(final int[] gArr, final double x, final double y)
    {
        return gArr[0] * x + gArr[1] * y;
    }

    public static double lerp2(final double deltaX, final double deltaY, final double d, final double e, final double f, final double g)
    {
        return lerp(deltaY, lerp(deltaX, d, e), lerp(deltaX, f, g));
    }

    // 1D
    public double sample1d(final double x)
    {
        final double f = x + this.originX;
        final int i = floor(f);
        final double l = f - i;
        final double o = perlinFade(l);
        return this.sample1d(i, l, o) + 0.5;
    }

    private double sample1d(final int sectionX, final double localX, final double fadeLocalX)
    {
        final double d = grad1d(this.getGradient(sectionX), localX);
        final double e = grad1d(this.getGradient(sectionX + 1), localX - 1.0D);
        return lerp(fadeLocalX, d, e);
    }

    private static double grad1d(final int hash, final double x)
    {
        return ((hash & 1) == 0) ? x : -x;
    }

    public static double lerp(final double delta, final double first, final double second)
    {
        return first + delta * (second - first);
    }

    // shared
    public int getGradient(final int hash)
    {
        return this.permutations[hash & 255] & 255;
    }

    public static double perlinFade(final double d)
    {
        return d * d * d * (d * (d * 6.0D - 15.0D) + 10.0D);
    }

    public static int floor(final double d)
    {
        final int i = (int) d;
        return d < i ? i - 1 : i;
    }
}