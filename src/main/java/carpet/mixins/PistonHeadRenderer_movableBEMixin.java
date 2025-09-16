package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.PistonHeadRenderStateInterface;
import carpet.fakes.PistonBlockEntityInterface;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRenderer_movableBEMixin implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState>
{
    BlockEntityRenderDispatcher dispatcher;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInitCM(CallbackInfo ci)
    {
        dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("RETURN"))
    private void addBE(PistonMovingBlockEntity pistonMovingBlockEntity, PistonHeadRenderState pistonHeadRenderState, float f, Vec3 vec3, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci)
    {
        if (CarpetSettings.movableBlockEntities)
        {
            BlockEntity be = ((PistonBlockEntityInterface) pistonMovingBlockEntity).getCarriedBlockEntity();
            if (be != null)
            {
                float progress = pistonMovingBlockEntity.getProgress(f);
                //System.out.println("progress: " + progress);
                //if (progress != 0) {
                    BlockEntityRenderState res = dispatcher.tryExtractRenderState(be, f, crumblingOverlay);
                    if (res != null) {
                        ((PistonHeadRenderStateInterface) pistonHeadRenderState).setMovedBERenderState(res);
                    }
                //}
            }
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitMovingBlock(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/MovingBlockRenderState;)V",
            ordinal = 0))
    private void endMethod(PistonHeadRenderState pistonHeadRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci)
    //private void endMethod(PistonMovingBlockEntity pistonBlockEntity_1, float partialTicks, PoseStack matrixStack_1, int i, int j, Vec3 vec3, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, SubmitNodeCollector submitNodeCollector, CallbackInfo ci)
    //private void endMethod3576(PistonMovingBlockEntity pistonBlockEntity_1, float partialTicks, PoseStack matrixStack_1, MultiBufferSource layeredVertexConsumerStorage_1, int int_1, int init_2, Vec3 cameraPos, CallbackInfo ci)
    {
        if (pistonHeadRenderState instanceof PistonHeadRenderStateInterface mbrsi && pistonHeadRenderState.block != null)
            if (mbrsi.getMovedBERenderState() != null) {
                BlockEntityRenderState res = mbrsi.getMovedBERenderState();
                if (res != null) {
                    dispatcher.submit(res, poseStack, submitNodeCollector, cameraRenderState);
                }
            }
    }
}
