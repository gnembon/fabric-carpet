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
    @Shadow private /*@Nullable*/ ShaderEffect transparencyShader;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci)
    {
        CarpetClient.shapes = new ShapesRenderer(client);
    }

    @Inject(method = "render", at =  @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/render/Camera;)V",
            shift = At.Shift.AFTER
    ))
    private void renderScarpetThings(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci)
    {
        // in normal circumstances we want to render shapes at the very end so it appears correctly behind stuff.
        if (!FabricAPIHooks.WORLD_RENDER_EVENTS && CarpetClient.shapes != null && transparencyShader == null)
        {
            RenderSystem.pushMatrix();
            CarpetClient.shapes.render(camera, tickDelta);
            RenderSystem.popMatrix();
        }
    }

    @Inject(method = "render", at =  @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDD)V",
            shift = At.Shift.AFTER
    ))
    private void renderScarpetThingsFabulously(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci)
    {
        // with fabulous graphics - stuff doesn't work this way for some reason - need to render with chunk lines.
        if (!FabricAPIHooks.WORLD_RENDER_EVENTS && CarpetClient.shapes != null && transparencyShader != null)
        {
            RenderSystem.pushMatrix();
            CarpetClient.shapes.render(camera, tickDelta);
            RenderSystem.popMatrix();
        }
    }
}
