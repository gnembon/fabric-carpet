package carpet.mixins;

import carpet.CarpetSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(StructureBlockBlockEntity.class)
public abstract class StructureBlockBlockEntity_limitsMixin
{
    @ModifyConstant(
            method = "fromTag",
            constant = @Constant(intValue = 48)
    )
    private int positiveLimit(int original) {
        return CarpetSettings.structureBlockLimit;
    }

    @ModifyConstant(
            method = "fromTag",
            constant = @Constant(intValue = -48)
    )
    private int negativeLimit(int original) {
        return -CarpetSettings.structureBlockLimit;
    }

    @ModifyArg(
            method = "saveStructure(Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/structure/Structure;saveFromWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/block/Block;)V"
            ),
            index = 4
    )
    private Block ignoredBlock(Block original) {
        return CarpetSettings.structureBlockIgnoredBlock;
    }

    @Environment(EnvType.CLIENT)
    @ModifyConstant(
            method = "getSquaredRenderDistance",
            constant = @Constant(doubleValue = 96d)
    )
    private double outlineRenderDistanceLimit(double original) {
        return CarpetSettings.structureBlockOutlineDistance;
    }
}
