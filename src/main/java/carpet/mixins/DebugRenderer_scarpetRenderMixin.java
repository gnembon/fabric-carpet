package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.script.utils.ShapesRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRenderer_scarpetRenderMixin
{
    private ShapesRenderer shapes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addRenderers(MinecraftClient client, CallbackInfo ci)
    {
        shapes = new ShapesRenderer(client);
        CarpetClient.shapes = shapes;
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void resetScarpetRenderes(CallbackInfo ci)
    {
        shapes.reset();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderScarpetThings(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, double cameraX, double cameraY, double cameraZ, CallbackInfo ci)
    {
        shapes.render(matrices, vertexConsumers, cameraX, cameraY, cameraZ);
    }

}
