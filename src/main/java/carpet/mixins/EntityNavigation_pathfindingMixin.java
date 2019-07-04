package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.PathfindingVisualizer;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(EntityNavigation.class)
public abstract class EntityNavigation_pathfindingMixin
{

    @Shadow @Final protected MobEntity entity;


    @Shadow /*@Nullable*/ protected abstract Path findPathTo(Set<BlockPos> set_1, int int_1, boolean boolean_1, int int_2);

    @Redirect(method =  "findPathTo(Lnet/minecraft/util/math/BlockPos;I)Lnet/minecraft/entity/ai/pathing/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;findPathTo(Ljava/util/Set;IZI)Lnet/minecraft/entity/ai/pathing/Path;"
    ))
    private Path pathToBlock(EntityNavigation entityNavigation, Set<BlockPos> set_1, int int_1, boolean boolean_1, int int_2)
    {
        if (!LoggerRegistry.__pathfinding)
            return findPathTo(set_1, int_1, boolean_1, int_2);
        long start = System.nanoTime();
        Path path = findPathTo(set_1, int_1, boolean_1, int_2);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        set_1.forEach(b -> PathfindingVisualizer.slowPath(entity, new Vec3d(b), duration, path != null));
        return path;
    }

    @Redirect(method =  "findPathTo(Lnet/minecraft/entity/Entity;I)Lnet/minecraft/entity/ai/pathing/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;findPathTo(Ljava/util/Set;IZI)Lnet/minecraft/entity/ai/pathing/Path;"
    ))
    private Path pathToEntity(EntityNavigation entityNavigation, Set<BlockPos> set_1, int int_1, boolean boolean_1, int int_2)
    {
        if (!LoggerRegistry.__pathfinding)
            return findPathTo(set_1, int_1, boolean_1, int_2);
        long start = System.nanoTime();
        Path path = findPathTo(set_1, int_1, boolean_1, int_2);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        set_1.forEach(b -> PathfindingVisualizer.slowPath(entity, new Vec3d(b), duration, path != null));
        return path;
    }
}
