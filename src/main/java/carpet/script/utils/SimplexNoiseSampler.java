package carpet.script.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.Mth;

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

    public static SimplexNoiseSampler getSimplex(long aLong)
    {
        if (samplers.size() > 256)
        {
            samplers.clear();
        }
        return samplers.computeIfAbsent(aLong, seed -> new SimplexNoiseSampler(new Random(seed)));
    }

    public SimplexNoiseSampler(Random random)
    {
        super(random);
    }

    private double grad(int hash, double x, double y, double z, double d)
    {
        double e = d - x * x - y * y - z * z;
        double g;
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
        double d = (x + y) * SKEW_FACTOR_2D;
        int i = Mth.floor(x + d);
        int j = Mth.floor(y + d);
        double e = (i + j) * UNSKEW_FACTOR_2D;
        double f = i - e;
        double g = j - e;
        double h = x - f;
        double k = y - g;
        byte n;
        byte o;
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

        double p = h - n + UNSKEW_FACTOR_2D;
        double q = k - o + UNSKEW_FACTOR_2D;
        double r = h - 1.0D + 2.0D * UNSKEW_FACTOR_2D;
        double s = k - 1.0D + 2.0D * UNSKEW_FACTOR_2D;
        int t = i & 255;
        int u = j & 255;
        int v = this.getGradient(t + this.getGradient(u)) % 12;
        int w = this.getGradient(t + n + this.getGradient(u + o)) % 12;
        int z = this.getGradient(t + 1 + this.getGradient(u + 1)) % 12;
        double aa = this.grad(v, h, k, 0.0D, 0.5D);
        double ab = this.grad(w, p, q, 0.0D, 0.5D);
        double ac = this.grad(z, r, s, 0.0D, 0.5D);
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
        double h = (d + e + f) * 0.3333333333333333D;
        int i = Mth.floor(d + h);
        int j = Mth.floor(e + h);
        int k = Mth.floor(f + h);
        //final double l = 0.16666666666666666D;
        double m = (i + j + k) * 0.16666666666666666D;
        double n = i - m;
        double o = j - m;
        double p = k - m;
        double q = d - n;
        double r = e - o;
        double s = f - p;
        byte z;
        byte aa;
        byte ab;
        byte ac;
        byte ad;
        byte bc;
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

        double bd = q - z + 0.16666666666666666D;
        double be = r - aa + 0.16666666666666666D;
        double bf = s - ab + 0.16666666666666666D;
        double bg = q - ac + 0.3333333333333333D;
        double bh = r - ad + 0.3333333333333333D;
        double bi = s - bc + 0.3333333333333333D;
        double bj = q - 1.0D + 0.5D;
        double bk = r - 1.0D + 0.5D;
        double bl = s - 1.0D + 0.5D;
        int bm = i & 255;
        int bn = j & 255;
        int bo = k & 255;
        int bp = this.getGradient(bm + this.getGradient(bn + this.getGradient(bo))) % 12;
        int bq = this.getGradient(bm + z + this.getGradient(bn + aa + this.getGradient(bo + ab))) % 12;
        int br = this.getGradient(bm + ac + this.getGradient(bn + ad + this.getGradient(bo + bc))) % 12;
        int bs = this.getGradient(bm + 1 + this.getGradient(bn + 1 + this.getGradient(bo + 1))) % 12;
        double bt = this.grad(bp, q, r, s, 0.6D);
        double bu = this.grad(bq, bd, be, bf, 0.6D);
        double bv = this.grad(br, bg, bh, bi, 0.6D);
        double bw = this.grad(bs, bj, bk, bl, 0.6D);
        return 16.0D * (bt + bu + bv + bw) + 0.5;
    }

    static
    {
        SKEW_FACTOR_2D = 0.5D * (sqrt3 - 1.0D);
        UNSKEW_FACTOR_2D = (3.0D - sqrt3) / 6.0D;
    }
}