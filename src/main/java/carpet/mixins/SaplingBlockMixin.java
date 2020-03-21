package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.BlockSaplingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin
{
    @Inject(method = "generate", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/block/sapling/SaplingGenerator;generate(Lnet/minecraft/world/IWorld;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Ljava/util/Random;)Z"),
            cancellable = true)
    private void onGenerate(ServerWorld serverWorld_1, BlockPos blockPos_1, BlockState blockState_1, Random random_1, CallbackInfo ci)
    {
        if(CarpetSettings.desertShrubs && serverWorld_1.getBiome(blockPos_1) == Biomes.DESERT && !BlockSaplingHelper.hasWater(serverWorld_1, blockPos_1))
        {
            serverWorld_1.setBlockState(blockPos_1, Blocks.DEAD_BUSH.getDefaultState(), 3);
            ci.cancel();
        }
    }
}
