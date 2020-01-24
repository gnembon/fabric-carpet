package carpet.script.utils;

import java.util.Random;

public class SimplexNoiseSampler extends PerlinNoiseSampler {
    private static final double sqrt3 = Math.sqrt(3.0D);
    private static final double SKEW_FACTOR_2D;
    private static final double UNSKEW_FACTOR_2D;

    public static SimplexNoiseSampler instance = new SimplexNoiseSampler(new Random(0));

    public SimplexNoiseSampler(Random random) {
        super(random);
    }

    private double grad(int hash, double x, double y, double z, double d) {
        double e = d - x * x - y * y - z * z;
        double g;
        if (e < 0.0D) {
            g = 0.0D;
        } else {
            e *= e;
            g = e * e * PerlinNoiseSampler.dot3d(PerlinNoiseSampler.gradients3d[hash], x, y, z);
        }

        return g;
    }

    public double sample(double x, double y) {
        double d = (x + y) * SKEW_FACTOR_2D;
        int i = PerlinNoiseSampler.floor(x + d);
        int j = PerlinNoiseSampler.floor(y + d);
        double e = (double)(i + j) * UNSKEW_FACTOR_2D;
        double f = (double)i - e;
        double g = (double)j - e;
        double h = x - f;
        double k = y - g;
        byte n;
        byte o;
        if (h > k) {
            n = 1;
            o = 0;
        } else {
            n = 0;
            o = 1;
        }

        double p = h - (double)n + UNSKEW_FACTOR_2D;
        double q = k - (double)o + UNSKEW_FACTOR_2D;
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
        return 70.0D * (aa + ab + ac);
    }

    static {
        SKEW_FACTOR_2D = 0.5D * (sqrt3 - 1.0D);
        UNSKEW_FACTOR_2D = (3.0D - sqrt3) / 6.0D;
    }
}