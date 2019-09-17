package carpet.mixins;

import carpet.fakes.MinecraftClientInferface;
import carpet.helpers.TickSpeed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRenderer_pausedShakeMixin
{
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "renderCenter", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderEntities(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/VisibleRegion;F)V"
    ))
    private void renderEntitiesNoShake(WorldRenderer worldRenderer, Camera camera_1, VisibleRegion visibleRegion_1, float float_1)
    {
        if (!TickSpeed.process_entities)
        {
            worldRenderer.renderEntities(camera_1, visibleRegion_1, ((MinecraftClientInferface)client).getPausedTickDelta());
        }
        else
        {
            worldRenderer.renderEntities(camera_1, visibleRegion_1, float_1);
        }
    }
}
