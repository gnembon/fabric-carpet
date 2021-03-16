package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.script.utils.ShapesRenderer;
import carpet.utils.FabricAPIHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRenderer_scarpetRenderMixin
{
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci)
    {
        CarpetClient.shapes = new ShapesRenderer(client);
    }

    @Inject(method = "render", at =  @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEffectVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;", shift = At.Shift.BEFORE
            //target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/render/Camera;)V", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/BackgroundRenderer;method_23792()V", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;", shift = At.Shift.AFTER
            //target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/render/Camera;)V", shift = At.Shift.AFTER // before return
    ))
    private void renderScarpetThings(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci)
    {
        // in normal circumstances we want to render shapes at the very end so it appears correctly behind stuff.
        // we might actually not need to play with render hooks here.
        if (!FabricAPIHooks.WORLD_RENDER_EVENTS && CarpetClient.shapes != null )
        {
            matrices.push();
            CarpetClient.shapes.render(matrices, camera, tickDelta);
            matrices.pop();
            RenderSystem.applyModelViewMatrix();

        }
    }
}
