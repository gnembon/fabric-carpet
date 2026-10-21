package carpet.mixins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin
{
    @Shadow private Component footer;

    @Shadow private Component header;

    @Unique
    private boolean hasFooterOrHeader()
    {
        return footer != null || header != null;
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean onDraw(Minecraft minecraftClient)
    {
        return this.minecraft.isLocalServer() && !this.hasFooterOrHeader();
    }
}