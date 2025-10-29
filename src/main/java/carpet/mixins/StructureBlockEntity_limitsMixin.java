package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.List;
import java.util.stream.Stream;

@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockEntity_limitsMixin
{
    @ModifyConstant(
            method = "loadAdditional",
            constant = @Constant(intValue = StructureBlockEntity.MAX_SIZE_PER_AXIS)
    )
    private int positiveLimit(int original) {
        return CarpetSettings.structureBlockLimit;
    }

    @ModifyConstant(
            method = "loadAdditional",
            constant = @Constant(intValue = -StructureBlockEntity.MAX_SIZE_PER_AXIS)
    )
    private int negativeLimit(int original) {
        return -CarpetSettings.structureBlockLimit;
    }

    @ModifyArg(
            method = "saveStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Vec3i;ZLjava/lang/String;ZLjava/util/List;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;fillFromWorld(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Vec3i;ZLjava/util/List;)V"
            ),
            index = 4
    )
    private static List<Block> ignoredBlock(List<Block> original) {
        if (original.contains(CarpetSettings.structureBlockIgnoredBlock))
            return original;
        return Stream.concat(original.stream(), Stream.of(CarpetSettings.structureBlockIgnoredBlock)).toList();
    }
}
