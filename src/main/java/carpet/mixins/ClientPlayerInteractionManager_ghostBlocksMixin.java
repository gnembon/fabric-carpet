package carpet.mixins;


import carpet.settings.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.packet.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManager_ghostBlocksMixin
{

    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "breakBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    )
    private void requestServerStatusForBlock(Block block, IWorld iWorld_1, BlockPos blockPos_1, BlockState blockState_1) {
        if ((CarpetSettings.miningGhostBlockFix || (
                    !client.isInSingleplayer() &&
                            !client.isIntegratedServerRunning() &&
                            client.getNetworkHandler().getCommandDispatcher().getRoot().getChild("carpet") == null)) &&
                blockState_1.getHardness(client.world, blockPos_1) > 0.0 &&
                blockState_1.getCollisionShape(iWorld_1, blockPos_1) != VoxelShapes.empty()
        )
        {
            ItemStack handItem = client.player.getMainHandStack();
            if (handItem.isEmpty() ||
                    handItem.getItem().getGroup() == ItemGroup.TOOLS ||
                    handItem.getItem().getGroup() == ItemGroup.COMBAT
            )
                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(new Vec3d(blockPos_1), Direction.DOWN, blockPos_1, false)
            ));
        }
    }
}