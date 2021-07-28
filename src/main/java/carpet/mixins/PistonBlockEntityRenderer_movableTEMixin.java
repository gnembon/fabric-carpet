package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.PistonBlockEntityInterface;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.PistonBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonBlockEntityRenderer.class)
public abstract class PistonBlockEntityRenderer_movableTEMixin implements BlockEntityRenderer<PistonBlockEntity>
{
    BlockEntityRenderDispatcher dispatcher;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInitCM(BlockEntityRendererFactory.Context arguments, CallbackInfo ci)
    {
        dispatcher = arguments.getRenderDispatcher();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/entity/PistonBlockEntityRenderer;renderModel(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;ZI)V", ordinal = 3))
    private void updateRenderBool(PistonBlockEntity pistonBlockEntity_1, float float_1, MatrixStack matrixStack_1, VertexConsumerProvider layeredVertexConsumerStorage_1, int int_1, int int_2, CallbackInfo ci)
    //private void updateRenderBool(PistonBlockEntity pistonBlockEntity_1, double double_1, double double_2, double double_3, float float_1, class_4587 class_4587_1, class_4597 class_4597_1, int int_1, CallbackInfo ci)
    {
        if (!((PistonBlockEntityInterface) pistonBlockEntity_1).isRenderModeSet())
            ((PistonBlockEntityInterface) pistonBlockEntity_1).setRenderCarriedBlockEntity(CarpetSettings.movableBlockEntities && ((PistonBlockEntityInterface) pistonBlockEntity_1).getCarriedBlockEntity() != null);
    }


    @Inject(method = "render", at = @At("RETURN"), locals = LocalCapture.NO_CAPTURE)
    private void endMethod3576(PistonBlockEntity pistonBlockEntity_1, float partialTicks, MatrixStack matrixStack_1, VertexConsumerProvider layeredVertexConsumerStorage_1, int int_1, int init_2, CallbackInfo ci)
    {
        if (((PistonBlockEntityInterface) pistonBlockEntity_1).getRenderCarriedBlockEntity())
        {
            BlockEntity carriedBlockEntity = ((PistonBlockEntityInterface) pistonBlockEntity_1).getCarriedBlockEntity();
            if (carriedBlockEntity != null)
            {
                // maybe ??? carriedBlockEntity.setPos(pistonBlockEntity_1.getPos());
                //((BlockEntityRenderDispatcherInterface) BlockEntityRenderDispatcher.INSTANCE).renderBlockEntityOffset(carriedBlockEntity, float_1, int_1, BlockRenderLayer.field_20799, bufferBuilder_1, pistonBlockEntity_1.getRenderOffsetX(float_1), pistonBlockEntity_1.getRenderOffsetY(float_1), pistonBlockEntity_1.getRenderOffsetZ(float_1));
                matrixStack_1.translate(
                        pistonBlockEntity_1.getRenderOffsetX(partialTicks),
                        pistonBlockEntity_1.getRenderOffsetY(partialTicks),
                        pistonBlockEntity_1.getRenderOffsetZ(partialTicks)
                );
                dispatcher.render(carriedBlockEntity, partialTicks, matrixStack_1, layeredVertexConsumerStorage_1);

            }
        }
    }
}
