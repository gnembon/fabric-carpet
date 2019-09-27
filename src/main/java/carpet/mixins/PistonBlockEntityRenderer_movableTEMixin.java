package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.PistonBlockEntityInterface;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.PistonBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonBlockEntityRenderer.class)
public abstract class PistonBlockEntityRenderer_movableTEMixin extends BlockEntityRenderer<PistonBlockEntity>
{
    public PistonBlockEntityRenderer_movableTEMixin(BlockEntityRenderDispatcher blockEntityRenderDispatcher_1)
    {
        super(blockEntityRenderDispatcher_1);
    }

    @Inject(method = "method_3576", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/entity/PistonBlockEntityRenderer;method_3575(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;Lnet/minecraft/world/World;Z)V", ordinal = 3))
    private void updateRenderBool(PistonBlockEntity pistonBlockEntity_1, double double_1, double double_2, double double_3, float float_1, class_4587 class_4587_1, class_4597 class_4597_1, int int_1, CallbackInfo ci)
    {
        if (!((PistonBlockEntityInterface) pistonBlockEntity_1).isRenderModeSet())
            ((PistonBlockEntityInterface) pistonBlockEntity_1).setRenderCarriedBlockEntity(CarpetSettings.movableBlockEntities && ((PistonBlockEntityInterface) pistonBlockEntity_1).getCarriedBlockEntity() != null);
    }


    @Inject(method = "method_3576", at = @At("RETURN"), locals = LocalCapture.NO_CAPTURE)
    private void endMethod3576(PistonBlockEntity pistonBlockEntity_1, double xOffset, double yOffset, double zOffset, float partialTicks, class_4587 transform, class_4597 bufferWrapper, int int_1, CallbackInfo ci)
    {
        if (((PistonBlockEntityInterface) pistonBlockEntity_1).getRenderCarriedBlockEntity())
        {
            BlockEntity carriedBlockEntity = ((PistonBlockEntityInterface) pistonBlockEntity_1).getCarriedBlockEntity();
            if (carriedBlockEntity != null)
            {
                carriedBlockEntity.setPos(pistonBlockEntity_1.getPos());
                //((BlockEntityRenderDispatcherInterface) BlockEntityRenderDispatcher.INSTANCE).renderBlockEntityOffset(carriedBlockEntity, float_1, int_1, BlockRenderLayer.field_20799, bufferBuilder_1, pistonBlockEntity_1.getRenderOffsetX(float_1), pistonBlockEntity_1.getRenderOffsetY(float_1), pistonBlockEntity_1.getRenderOffsetZ(float_1));
                transform.method_22904( // translate
                        pistonBlockEntity_1.getRenderOffsetX(partialTicks),
                        pistonBlockEntity_1.getRenderOffsetY(partialTicks),
                        pistonBlockEntity_1.getRenderOffsetZ(partialTicks)
                );
                BlockEntityRenderDispatcher.INSTANCE.render(carriedBlockEntity, partialTicks, transform, bufferWrapper, xOffset, yOffset, zOffset);

            }
        }
    }
}
