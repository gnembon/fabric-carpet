package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.script.utils.ShapesRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRenderer_scarpetRenderMixin
{
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci)
    {
        CarpetClient.shapes = new ShapesRenderer(minecraft);
    }

    @Inject(method = "renderLevel", at =  @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;", shift = At.Shift.BEFORE
            //target = "Lnet/minecraft/client/render/RenderLayer;getWaterMask()Lnet/minecraft/client/render/RenderLayer;", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEffectVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;", shift = At.Shift.BEFORE
            //target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/render/Camera;)V", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/BackgroundRenderer;method_23792()V", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/render/Camera;)V", shift = At.Shift.AFTER // before return
    ))
    private void renderScarpetThings(PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci)
    {
        // in normal circumstances we want to render shapes at the very end so it appears correctly behind stuff.
        // we might actually not need to play with render hooks here.
        //if (!FabricAPIHooks.WORLD_RENDER_EVENTS && CarpetClient.shapes != null )
        if (CarpetClient.shapes != null)
        {
            CarpetClient.shapes.render(matrices, camera, tickDelta);
        }
    }
}
