package carpet.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;

public class Tracer
{
    public static HitResult traceEntityLook(Entity entity, double reach) {
        Vec3d vec3d_1 = entity.getCameraPosVec(1.0F);
        Vec3d vec3d_2 = entity.getRotationVec(1.0F);
        Vec3d vec3d_3 = vec3d_1.add(vec3d_2.x * reach, vec3d_2.y * reach, vec3d_2.z * reach);
        return entity.world.rayTrace(new RayTraceContext(vec3d_1, vec3d_3, RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.ANY, entity));
    }
}
