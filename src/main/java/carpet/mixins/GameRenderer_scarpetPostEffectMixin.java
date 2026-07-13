package carpet.mixins;

import carpet.script.utils.PostEffectDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRenderer_scarpetPostEffectMixin
{
    @Invoker("setSpectatedEntityPostEffect")
    public abstract void invokeSetPostEffect(Identifier id);

    // Vanilla clears the post effect whenever the player leaves first person (see Minecraft#handleKeybinds,
    // which calls this with a null camera entity on that transition) or switches camera entity. A scarpet-applied
    // effect must survive both, so this checks the real camera entity ourselves instead of trusting the argument,
    // and only lets vanilla's creeper/spider/enderman/clear logic run when no scarpet effect is active.
    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void onCheckEntityPostEffect(Entity cameraEntity, CallbackInfo ci)
    {
        Minecraft client = Minecraft.getInstance();
        Entity actualCamera = client.getCameraEntity();
        if (actualCamera != null && actualCamera == client.player)
        {
            Identifier custom = PostEffectDispatcher.getActiveEffect(actualCamera.level().getGameTime());
            if (custom != null)
            {
                invokeSetPostEffect(custom);
                ci.cancel();
            }
        }
    }
}
