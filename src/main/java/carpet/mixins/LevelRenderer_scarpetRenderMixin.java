package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.script.utils.ShapesRenderer;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRenderer_scarpetRenderMixin
{
    @Shadow @Final private LevelRenderState levelRenderState;

    @Shadow @Final private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, ModelManager modelManager, TextureManager textureManager, AtlasManager atlasManager, ShaderManager shaderManager, GameRenderer gameRenderer, int width, int height, CallbackInfo ci)
    {
        CarpetClient.shapes = new ShapesRenderer(Minecraft.getInstance());
    }

    // Scarpet shapes must be submitted in the same phase where vanilla submits its features, i.e. before
    // featureRenderDispatcher.prepareFrame drains submitNodeStorage for this frame. Submitting later (the
    // previous approach used a dedicated FramePass, which executes only during frame-graph execution, after
    // the drain) means the submissions are only picked up by the NEXT frame's prepareFrame - and since
    // block/item/text submissions bake the camera-relative translation into their PoseStack at submit time,
    // they got drawn with a one-frame-stale camera offset, visibly drifting whenever the camera moved.
    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;submitFeatures(Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
            shift = At.Shift.AFTER
    ))
    private void submitScarpetShapes(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci)
    {
        if (CarpetClient.shapes != null)
        {
            final float deltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            CarpetClient.shapes.render(submitNodeStorage, levelRenderState, modelViewMatrix, deltaPartialTick);
        }
    }
}
