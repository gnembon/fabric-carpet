package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.PistonBlockEntityInterface;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRenderer_movableBEMixin implements BlockEntityRenderer<PistonMovingBlockEntity>
{
    BlockEntityRenderDispatcher dispatcher;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInitCM(BlockEntityRendererProvider.Context arguments, CallbackInfo ci)
    {
        dispatcher = arguments.getBlockEntityRenderDispatcher();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;renderBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;ZI)V", ordinal = 3))
    private void updateRenderBool(PistonMovingBlockEntity pistonBlockEntity_1, float float_1, PoseStack matrixStack_1, MultiBufferSource layeredVertexConsumerStorage_1, int int_1, int int_2, CallbackInfo ci)
    //private void updateRenderBool(PistonBlockEntity pistonBlockEntity_1, double double_1, double double_2, double double_3, float float_1, class_4587 class_4587_1, class_4597 class_4597_1, int int_1, CallbackInfo ci)
    {
        if (!((PistonBlockEntityInterface) pistonBlockEntity_1).isRenderModeSet())
            ((PistonBlockEntityInterface) pistonBlockEntity_1).setRenderCarriedBlockEntity(CarpetSettings.movableBlockEntities && ((PistonBlockEntityInterface) pistonBlockEntity_1).getCarriedBlockEntity() != null);
    }


    @Inject(method = "render", at = @At("RETURN"), locals = LocalCapture.NO_CAPTURE)
    private void endMethod3576(PistonMovingBlockEntity pistonBlockEntity_1, float partialTicks, PoseStack matrixStack_1, MultiBufferSource layeredVertexConsumerStorage_1, int int_1, int init_2, CallbackInfo ci)
    {
        if (((PistonBlockEntityInterface) pistonBlockEntity_1).getRenderCarriedBlockEntity())
        {
            BlockEntity carriedBlockEntity = ((PistonBlockEntityInterface) pistonBlockEntity_1).getCarriedBlockEntity();
            if (carriedBlockEntity != null)
            {
                // maybe ??? carriedBlockEntity.setPos(pistonBlockEntity_1.getPos());
                //((BlockEntityRenderDispatcherInterface) BlockEntityRenderDispatcher.INSTANCE).renderBlockEntityOffset(carriedBlockEntity, float_1, int_1, BlockRenderLayer.field_20799, bufferBuilder_1, pistonBlockEntity_1.getRenderOffsetX(float_1), pistonBlockEntity_1.getRenderOffsetY(float_1), pistonBlockEntity_1.getRenderOffsetZ(float_1));
                matrixStack_1.translate(
                        pistonBlockEntity_1.getXOff(partialTicks),
                        pistonBlockEntity_1.getYOff(partialTicks),
                        pistonBlockEntity_1.getZOff(partialTicks)
                );
                dispatcher.render(carriedBlockEntity, partialTicks, matrixStack_1, layeredVertexConsumerStorage_1);

            }
        }
    }
}
