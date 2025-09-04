package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.script.utils.ShapesRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRenderer_scarpetRenderMixin
{
    @Shadow @Final private LevelTargetBundle targets;

    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow @Final private RenderBuffers renderBuffers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, LevelRenderState levelRenderState, FeatureRenderDispatcher featureRenderDispatcher, CallbackInfo ci)
    {
        CarpetClient.shapes = new ShapesRenderer(minecraft);
    }

    @Inject(method = "addMainPass", at = @At("RETURN"))
    private void renderScarpetThingsLate(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Camera camera, Matrix4f matrix4f, GpuBufferSlice gpuBufferSlice, boolean bl, LevelRenderState levelRenderState, DeltaTracker deltaTracker, ProfilerFiller profilerFiller, CallbackInfo ci)
    {
        // in normal circumstances we want to render shapes at the very end so it appears correctly behind stuff.
        // we might actually not need to play with render hooks here.
        //if (!FabricAPIHooks.WORLD_RENDER_EVENTS && CarpetClient.shapes != null )
        if (CarpetClient.shapes != null)
        {
            final float deltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            FramePass pass = frameGraphBuilder.addPass("scarpet_shapes");
            targets.main = pass.readsAndWrites(targets.main);
            pass.executes(() -> CarpetClient.shapes.render(null, camera, deltaPartialTick));
            featureRenderDispatcher.renderAllFeatures();
            renderBuffers.bufferSource().endLastBatch();
        }
    }
}
