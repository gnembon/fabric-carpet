package carpet.script.utils;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Tracer
{
    public static HitResult rayTrace(final Entity source, final float partialTicks, final double reach, final boolean fluids)
    {
        final BlockHitResult blockHit = rayTraceBlocks(source, partialTicks, reach, fluids);
        double maxSqDist = reach * reach;
        if (blockHit != null)
        {
            maxSqDist = blockHit.getLocation().distanceToSqr(source.getEyePosition(partialTicks));
        }
        final EntityHitResult entityHit = rayTraceEntities(source, partialTicks, reach, maxSqDist);
        return entityHit == null ? blockHit : entityHit;
    }

    public static BlockHitResult rayTraceBlocks(final Entity source, final float partialTicks, final double reach, final boolean fluids)
    {
        final Vec3 pos = source.getEyePosition(partialTicks);
        final Vec3 rotation = source.getViewVector(partialTicks);
        final Vec3 reachEnd = pos.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
        return source.level.clip(new ClipContext(pos, reachEnd, ClipContext.Block.OUTLINE, fluids ?
                ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, source));
    }

    public static EntityHitResult rayTraceEntities(final Entity source, final float partialTicks, final double reach, final double maxSqDist)
    {
        final Vec3 pos = source.getEyePosition(partialTicks);
        final Vec3 reachVec = source.getViewVector(partialTicks).scale(reach);
        final AABB box = source.getBoundingBox().expandTowards(reachVec).inflate(1);
        return rayTraceEntities(source, pos, pos.add(reachVec), box, e -> !e.isSpectator() && e.isPickable(), maxSqDist);
    }

    public static EntityHitResult rayTraceEntities(final Entity source, final Vec3 start, final Vec3 end, final AABB box, final Predicate<Entity> predicate, final double maxSqDistance)
    {
        final Level world = source.level;
        double targetDistance = maxSqDistance;
        Entity target = null;
        Vec3 targetHitPos = null;
        for (final Entity current : world.getEntities(source, box, predicate))
        {
            final AABB currentBox = current.getBoundingBox().inflate(current.getPickRadius());
            final Optional<Vec3> currentHit = currentBox.clip(start, end);
            if (currentBox.contains(start))
            {
                if (targetDistance >= 0)
                {
                    target = current;
                    targetHitPos = currentHit.orElse(start);
                    targetDistance = 0;
                }
            }
            else if (currentHit.isPresent())
            {
                final Vec3 currentHitPos = currentHit.get();
                final double currentDistance = start.distanceToSqr(currentHitPos);
                if (currentDistance < targetDistance || targetDistance == 0)
                {
                    if (current.getRootVehicle() == source.getRootVehicle())
                    {
                        if (targetDistance == 0)
                        {
                            target = current;
                            targetHitPos = currentHitPos;
                        }
                    }
                    else
                    {
                        target = current;
                        targetHitPos = currentHitPos;
                        targetDistance = currentDistance;
                    }
                }
            }
        }
        return target == null ? null : new EntityHitResult(target, targetHitPos);
    }
}
