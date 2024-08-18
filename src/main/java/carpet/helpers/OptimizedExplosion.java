package carpet.helpers;
//Author: masa

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import carpet.logging.logHelpers.ExplosionLogHelper;
import carpet.mixins.ExplosionAccessor;
import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.tuple.Pair;

public class OptimizedExplosion
{
    // masa's optimizations
    private static Object2DoubleOpenHashMap<Pair<Vec3, AABB>> densityCache = new Object2DoubleOpenHashMap<>();
    private static Object2ObjectOpenHashMap<BlockPos, BlockState> stateCache = new Object2ObjectOpenHashMap<>();
    private static Object2ObjectOpenHashMap<BlockPos, FluidState> fluidCache = new Object2ObjectOpenHashMap<>();
    private static BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
    private static ObjectOpenHashSet<BlockPos> affectedBlockPositionsSet = new ObjectOpenHashSet<>();
    private static boolean firstRay;
    private static boolean rayCalcDone;

    public static List<BlockPos> doExplosionA(Explosion e, ExplosionLogHelper eLogger) {
        ExplosionAccessor eAccess = (ExplosionAccessor) e;

        List<BlockPos> toBlow;

        if (!CarpetSettings.explosionNoBlockDamage && eAccess.getDamageSource() != null) {
            rayCalcDone = false;
            firstRay = true;
            getAffectedPositionsOnPlaneY(e,  0,  0, 15,  0, 15); // bottom
            getAffectedPositionsOnPlaneY(e, 15,  0, 15,  0, 15); // top
            getAffectedPositionsOnPlaneX(e,  0,  1, 14,  0, 15); // west
            getAffectedPositionsOnPlaneX(e, 15,  1, 14,  0, 15); // east
            getAffectedPositionsOnPlaneZ(e,  0,  1, 14,  1, 14); // north
            getAffectedPositionsOnPlaneZ(e, 15,  1, 14,  1, 14); // south
            stateCache.clear();
            fluidCache.clear();

            toBlow = new ArrayList<>(affectedBlockPositionsSet);
            affectedBlockPositionsSet.clear();
        } else {
            toBlow = Collections.emptyList();
        }
        densityCache.clear();

        return toBlow;
    }

    private static void getAffectedPositionsOnPlaneX(Explosion e, int x, int yStart, int yEnd, int zStart, int zEnd)
    {
        if (!rayCalcDone)
        {
            final double xRel = (double) x / 15.0D * 2.0D - 1.0D;

            for (int z = zStart; z <= zEnd; ++z)
            {
                double zRel = (double) z / 15.0D * 2.0D - 1.0D;

                for (int y = yStart; y <= yEnd; ++y)
                {
                    double yRel = (double) y / 15.0D * 2.0D - 1.0D;

                    if (checkAffectedPosition(e, xRel, yRel, zRel))
                    {
                        return;
                    }
                }
            }
        }
    }

    private static void getAffectedPositionsOnPlaneY(Explosion e, int y, int xStart, int xEnd, int zStart, int zEnd)
    {
        if (!rayCalcDone)
        {
            final double yRel = (double) y / 15.0D * 2.0D - 1.0D;

            for (int z = zStart; z <= zEnd; ++z)
            {
                double zRel = (double) z / 15.0D * 2.0D - 1.0D;

                for (int x = xStart; x <= xEnd; ++x)
                {
                    double xRel = (double) x / 15.0D * 2.0D - 1.0D;

                    if (checkAffectedPosition(e, xRel, yRel, zRel))
                    {
                        return;
                    }
                }
            }
        }
    }

    private static void getAffectedPositionsOnPlaneZ(Explosion e, int z, int xStart, int xEnd, int yStart, int yEnd)
    {
        if (!rayCalcDone)
        {
            final double zRel = (double) z / 15.0D * 2.0D - 1.0D;

            for (int x = xStart; x <= xEnd; ++x)
            {
                double xRel = (double) x / 15.0D * 2.0D - 1.0D;

                for (int y = yStart; y <= yEnd; ++y)
                {
                    double yRel = (double) y / 15.0D * 2.0D - 1.0D;

                    if (checkAffectedPosition(e, xRel, yRel, zRel))
                    {
                        return;
                    }
                }
            }
        }
    }

    private static boolean checkAffectedPosition(Explosion e, double xRel, double yRel, double zRel)
    {
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        double len = Math.sqrt(xRel * xRel + yRel * yRel + zRel * zRel);
        double xInc = (xRel / len) * 0.3;
        double yInc = (yRel / len) * 0.3;
        double zInc = (zRel / len) * 0.3;
        float rand = eAccess.getLevel().random.nextFloat();
        float sizeRand = (CarpetSettings.tntRandomRange >= 0 ? (float) CarpetSettings.tntRandomRange : rand);
        float size = eAccess.getRadius() * (0.7F + sizeRand * 0.6F);
        Vec3 vec3 = eAccess.getCenter();
        double posX = vec3.x;
        double posY = vec3.y;
        double posZ = vec3.z;

        for (float f1 = 0.3F; size > 0.0F; size -= 0.22500001F)
        {
            posMutable.set(posX, posY, posZ);

            // Don't query already cached positions again from the world
            BlockState state = stateCache.get(posMutable);
            FluidState fluid = fluidCache.get(posMutable);
            BlockPos posImmutable = null;

            if (state == null)
            {
                posImmutable = posMutable.immutable();
                state = eAccess.getLevel().getBlockState(posImmutable);
                stateCache.put(posImmutable, state);
                fluid = eAccess.getLevel().getFluidState(posImmutable);
                fluidCache.put(posImmutable, fluid);
            }

            if (!state.isAir())
            {
                float resistance = Math.max(state.getBlock().getExplosionResistance(), fluid.getExplosionResistance());

                if (eAccess.getSource() != null)
                {
                    resistance = eAccess.getSource().getBlockExplosionResistance(e, eAccess.getLevel(), posMutable, state, fluid, resistance);
                }

                size -= (resistance + 0.3F) * 0.3F;
            }

            if (size > 0.0F)
            {
                if ((eAccess.getSource() == null || eAccess.getSource().shouldBlockExplode(e, eAccess.getLevel(), posMutable, state, size)))
                    affectedBlockPositionsSet.add(posImmutable != null ? posImmutable : posMutable.immutable());
            }
            else if (firstRay)
            {
                rayCalcDone = true;
                return true;
            }

            firstRay = false;

            posX += xInc;
            posY += yInc;
            posZ += zInc;
        }

        return false;
    }
}
