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

@Mixin(EntityNavigation.class)
public abstract class EntityNavigation_pathfindingMixin
{
    @Shadow /*@Nullable*/ protected abstract Path findPathTo(BlockPos blockPos_1, double double_1, double double_2, double double_3, int int_1, boolean boolean_1);

    @Shadow @Final protected MobEntity entity;

    @Redirect(method =  "findPathTo(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/entity/ai/pathing/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;findPathTo(Lnet/minecraft/util/math/BlockPos;DDDIZ)Lnet/minecraft/entity/ai/pathing/Path;"
    ))
    private Path pathToBlock(EntityNavigation entityNavigation, BlockPos blockPos_1, double double_1, double double_2, double double_3, int int_1, boolean boolean_1)
    {
        if (!LoggerRegistry.__pathfinding)
            return findPathTo(blockPos_1, double_1, double_2, double_3, int_1, boolean_1);
        long start = System.nanoTime();
        Path path = findPathTo(blockPos_1, double_1, double_2, double_3, int_1, boolean_1);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        PathfindingVisualizer.slowPath(entity, new Vec3d(double_1, double_2, double_3), duration, path != null);
        return path;
    }

    @Redirect(method =  "findPathTo(Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/ai/pathing/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;findPathTo(Lnet/minecraft/util/math/BlockPos;DDDIZ)Lnet/minecraft/entity/ai/pathing/Path;"
    ))
    private Path pathToEntity(EntityNavigation entityNavigation, BlockPos blockPos_1, double double_1, double double_2, double double_3, int int_1, boolean boolean_1)
    {
        if (!LoggerRegistry.__pathfinding)
            return findPathTo(blockPos_1, double_1, double_2, double_3, int_1, boolean_1);
        long start = System.nanoTime();
        Path path = findPathTo(blockPos_1, double_1, double_2, double_3, int_1, boolean_1);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        PathfindingVisualizer.slowPath(entity, new Vec3d(double_1, double_2, double_3), duration, path != null);
        return path;
    }
}
