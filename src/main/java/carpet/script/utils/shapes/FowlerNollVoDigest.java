package carpet.script.utils.shapes;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class FowlerNollVoDigest
{
    // using FNV-1a algorithm

    public static final long FNV_64_INIT = -3750763034362895579L;
    public static final long FNV_64_PRIME = 1099511628211L;

    public long digest;

    public static FowlerNollVoDigest of(Object ... o)
    {
        FowlerNollVoDigest fnv1a = new FowlerNollVoDigest();
        return fnv1a.with(o);
    }

    public FowlerNollVoDigest()
    {
        digest = FNV_64_INIT;
    }

    private void eat(long l)
    {
        digest = digest ^ l;
        digest = digest * FNV_64_PRIME;
    }

    public FowlerNollVoDigest with(Object ... os)
    {
        for (Object o: os)
        {
            if (o == null)
            {
                continue;
            }
            if (o instanceof Object[] oo)
            {
                with(oo);
                continue;
            }
            if (o instanceof Iterable oi)
            {
                for (Object oo: oi)
                {
                    with(oo);
                }
                continue;
            }
            eat(o.hashCode());
        }
        return this;
    }

    public long getDigest()
    {
        return digest;
    }

    public FowlerNollVoDigest withInt(int ... is)
    {
        for (int i: is)
        {
            eat(i);
        }
        return this;
    }

    public FowlerNollVoDigest withLong(long ... ls)
    {
        for (long l: ls)
        {
            eat(l);
        }
        return this;
    }

    public FowlerNollVoDigest withDbl(double ... ds)
    {
        for (double d: ds)
        {
            eat(Double.doubleToLongBits(d));
        }
        return this;
    }

    public FowlerNollVoDigest withFloat(float ... fs)
    {
        for (float f: fs)
        {
            eat(Float.floatToIntBits(f));
        }
        return this;
    }

    public FowlerNollVoDigest withBool(boolean ... bs)
    {
        for (boolean b: bs)
        {
            eat(b ? 1 : 0);
        }
        return this;
    }

    private static final double xdif = new Random('x').nextDouble();
    private static final double ydif = new Random('y').nextDouble();
    private static final double zdif = new Random('z').nextDouble();

    public FowlerNollVoDigest withCoords(Vec3 ... vecs)
    {
        for (Vec3 vec: vecs)
        {
            withDbl(vec.x+xdif, vec.y+ydif, vec.z+zdif);
        }
        return this;
    }
}
