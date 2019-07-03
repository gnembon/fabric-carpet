package carpet.mixins;

import carpet.fakes.PlayerListHudInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin
{
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow @Final private PlayerListHud playerListHud;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isInSingleplayer()Z"))
    private boolean onDraw(MinecraftClient minecraftClient)
    {
        return this.client.isInSingleplayer() && !((PlayerListHudInterface) playerListHud).hasFooterOrHeader();
    }

}
