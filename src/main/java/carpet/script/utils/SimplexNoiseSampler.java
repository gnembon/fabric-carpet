package carpet.script.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Map;
import java.util.Random;

// extracted from import net.minecraft.util.math.noise.SimplexNoiseSampler
public class SimplexNoiseSampler extends PerlinNoiseSampler
{
    private static final double sqrt3 = Math.sqrt(3.0D);
    private static final double SKEW_FACTOR_2D;
    private static final double UNSKEW_FACTOR_2D;

    public static SimplexNoiseSampler instance = new SimplexNoiseSampler(new Random(0));
    public static Map<Long, SimplexNoiseSampler> samplers = new Long2ObjectOpenHashMap<>();

    public static SimplexNoiseSampler getSimplex(final long aLong)
    {
        if (samplers.size() > 256)
        {
            samplers.clear();
        }
        return samplers.computeIfAbsent(aLong, seed -> new SimplexNoiseSampler(new Random(seed)));
    }

    public SimplexNoiseSampler(final Random random)
    {
        super(random);
    }

    private double grad(final int hash, final double x, final double y, final double z, final double d)
    {
        double e = d - x * x - y * y - z * z;
        final double g;
        if (e < 0.0D)
        {
            g = 0.0D;
        }
        else
        {
            e *= e;
            g = e * e * PerlinNoiseSampler.dot3d(PerlinNoiseSampler.gradients3d[hash], x, y, z);
        }

        return g;
    }

    @Override
    public double sample2d(double x, double y)
    {
        x = x / 2;
        y = y / 2;
        final double d = (x + y) * SKEW_FACTOR_2D;
        final int i = PerlinNoiseSampler.floor(x + d);
        final int j = PerlinNoiseSampler.floor(y + d);
        final double e = (i + j) * UNSKEW_FACTOR_2D;
        final double f = i - e;
        final double g = j - e;
        final double h = x - f;
        final double k = y - g;
        final byte n;
        final byte o;
        if (h > k)
        {
            n = 1;
            o = 0;
        }
        else
        {
            n = 0;
            o = 1;
        }

        final double p = h - n + UNSKEW_FACTOR_2D;
        final double q = k - o + UNSKEW_FACTOR_2D;
        final double r = h - 1.0D + 2.0D * UNSKEW_FACTOR_2D;
        final double s = k - 1.0D + 2.0D * UNSKEW_FACTOR_2D;
        final int t = i & 255;
        final int u = j & 255;
        final int v = this.getGradient(t + this.getGradient(u)) % 12;
        final int w = this.getGradient(t + n + this.getGradient(u + o)) % 12;
        final int z = this.getGradient(t + 1 + this.getGradient(u + 1)) % 12;
        final double aa = this.grad(v, h, k, 0.0D, 0.5D);
        final double ab = this.grad(w, p, q, 0.0D, 0.5D);
        final double ac = this.grad(z, r, s, 0.0D, 0.5D);
        //return 70.0D * (aa + ab + ac);
        return 35.0D * (aa + ab + ac) + 0.5;
    }

    @Override
    public double sample3d(double d, double e, double f)
    {
        d = d / 2;
        e = e / 2;
        f = f / 2;
        //final double g = 0.3333333333333333D;
        final double h = (d + e + f) * 0.3333333333333333D;
        final int i = floor(d + h);
        final int j = floor(e + h);
        final int k = floor(f + h);
        //final double l = 0.16666666666666666D;
        final double m = (i + j + k) * 0.16666666666666666D;
        final double n = i - m;
        final double o = j - m;
        final double p = k - m;
        final double q = d - n;
        final double r = e - o;
        final double s = f - p;
        final byte z;
        final byte aa;
        final byte ab;
        final byte ac;
        final byte ad;
        final byte bc;
        if (q >= r)
        {
            if (r >= s)
            {
                z = 1;
                aa = 0;
                ab = 0;
                ac = 1;
                ad = 1;
                bc = 0;
            }
            else if (q >= s)
            {
                z = 1;
                aa = 0;
                ab = 0;
                ac = 1;
                ad = 0;
                bc = 1;
            }
            else
            {
                z = 0;
                aa = 0;
                ab = 1;
                ac = 1;
                ad = 0;
                bc = 1;
            }
        }
        else if (r < s)
        {
            z = 0;
            aa = 0;
            ab = 1;
            ac = 0;
            ad = 1;
            bc = 1;
        }
        else if (q < s)
        {
            z = 0;
            aa = 1;
            ab = 0;
            ac = 0;
            ad = 1;
            bc = 1;
        }
        else
        {
            z = 0;
            aa = 1;
            ab = 0;
            ac = 1;
            ad = 1;
            bc = 0;
        }

        final double bd = q - z + 0.16666666666666666D;
        final double be = r - aa + 0.16666666666666666D;
        final double bf = s - ab + 0.16666666666666666D;
        final double bg = q - ac + 0.3333333333333333D;
        final double bh = r - ad + 0.3333333333333333D;
        final double bi = s - bc + 0.3333333333333333D;
        final double bj = q - 1.0D + 0.5D;
        final double bk = r - 1.0D + 0.5D;
        final double bl = s - 1.0D + 0.5D;
        final int bm = i & 255;
        final int bn = j & 255;
        final int bo = k & 255;
        final int bp = this.getGradient(bm + this.getGradient(bn + this.getGradient(bo))) % 12;
        final int bq = this.getGradient(bm + z + this.getGradient(bn + aa + this.getGradient(bo + ab))) % 12;
        final int br = this.getGradient(bm + ac + this.getGradient(bn + ad + this.getGradient(bo + bc))) % 12;
        final int bs = this.getGradient(bm + 1 + this.getGradient(bn + 1 + this.getGradient(bo + 1))) % 12;
        final double bt = this.grad(bp, q, r, s, 0.6D);
        final double bu = this.grad(bq, bd, be, bf, 0.6D);
        final double bv = this.grad(br, bg, bh, bi, 0.6D);
        final double bw = this.grad(bs, bj, bk, bl, 0.6D);
        return 16.0D * (bt + bu + bv + bw) + 0.5;
    }

    static
    {
        SKEW_FACTOR_2D = 0.5D * (sqrt3 - 1.0D);
        UNSKEW_FACTOR_2D = (3.0D - sqrt3) / 6.0D;
    }
}