package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.PathfindingVisualizer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

@Mixin(PathNavigation.class)
public abstract class PathNavigation_pathfindingMixin
{

    @Shadow @Final protected Mob mob;


    @Shadow protected @Nullable abstract Path createPath(Set<BlockPos> set, int i, boolean bl, int j);

    @Redirect(method =  "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;"
    ))
    private Path pathToBlock(PathNavigation entityNavigation, Set<BlockPos> set_1, int int_1, boolean boolean_1, int int_2)
    {
        if (!LoggerRegistry.__pathfinding)
            return createPath(set_1, int_1, boolean_1, int_2);
        long start = System.nanoTime();
        Path path = createPath(set_1, int_1, boolean_1, int_2);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        set_1.forEach(b -> PathfindingVisualizer.slowPath(mob, Vec3.atBottomCenterOf(b), duration, path != null)); // ground centered position
        return path;
    }

    @Redirect(method =  "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;"
    ))
    private Path pathToEntity(PathNavigation entityNavigation, Set<BlockPos> set_1, int int_1, boolean boolean_1, int int_2)
    {
        if (!LoggerRegistry.__pathfinding)
            return createPath(set_1, int_1, boolean_1, int_2);
        long start = System.nanoTime();
        Path path = createPath(set_1, int_1, boolean_1, int_2);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        set_1.forEach(b -> PathfindingVisualizer.slowPath(mob, Vec3.atBottomCenterOf(b), duration, path != null));
        return path;
    }
}
