package carpet.mixins;

import carpet.fakes.MinecraftClientInferface;
import carpet.helpers.TickSpeed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class WorldRenderer_pausedShakeMixin
{
    @Shadow @Final private MinecraftClient client;

    @ModifyVariable(method = "method_22710", argsOnly = true, ordinal = 0 ,at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getEntities()Ljava/lang/Iterable;"
    ))
    private float changeTickPhase(float previous)
    {
        if (!TickSpeed.process_entities)
            return ((MinecraftClientInferface)client).getPausedTickDelta();
        return previous;
    }
}
