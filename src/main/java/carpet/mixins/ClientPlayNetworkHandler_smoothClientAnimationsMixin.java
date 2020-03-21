package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandler_smoothClientAnimationsMixin
{
    @Shadow private ClientWorld world;
    // fix as suggested by G4me4u
    @Inject( method = "onChunkData", locals = LocalCapture.CAPTURE_FAILHARD, require = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;",
            shift = At.Shift.AFTER
    ))
    private void recreateMovingPistons(ChunkDataS2CPacket packet, CallbackInfo ci,
                                       Iterator var5, CompoundTag tag, BlockPos blockPos)
    {
        if (CarpetSettings.smoothClientAnimations)
        {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity == null && "minecraft:piston".equals(tag.getString("id")))
            {
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.getBlock() == Blocks.MOVING_PISTON) {
                    tag.putFloat("progress", Math.min(tag.getFloat("progress") + 0.5F, 1.0F));
                    blockEntity = new PistonBlockEntity();
                    blockEntity.fromTag(tag);
                    world.setBlockEntity(blockPos, blockEntity);
                    blockEntity.resetBlock();
                }
            }
        }
    }
}
