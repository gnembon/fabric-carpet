package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.script.utils.ShapesRenderer;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci)
    {
        CarpetClient.shapes = new ShapesRenderer(minecraft);
    }

    @Inject(method = "addParticlesPass", at = @At("RETURN"))
    private void renderScarpetThingsLate(FrameGraphBuilder frameGraphBuilder, Camera camera, LightTexture lightTexture, float f, FogParameters fogParameters, CallbackInfo ci)
    {
        // in normal circumstances we want to render shapes at the very end so it appears correctly behind stuff.
        // we might actually not need to play with render hooks here.
        //if (!FabricAPIHooks.WORLD_RENDER_EVENTS && CarpetClient.shapes != null )
        if (CarpetClient.shapes != null)
        {
            FramePass pass = frameGraphBuilder.addPass("scarpet_shapes");
            targets.main = pass.readsAndWrites(targets.main);
            pass.executes(() -> CarpetClient.shapes.render(null, camera, f));
        }
    }
}
