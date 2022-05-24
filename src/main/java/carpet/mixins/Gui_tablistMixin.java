package carpet.mixins;

import carpet.fakes.PlayerListHudInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class Gui_tablistMixin
{
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow @Final private PlayerTabOverlay tabList;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean onDraw(Minecraft minecraftClient)
    {
        return this.minecraft.isLocalServer() && !((PlayerListHudInterface) tabList).hasFooterOrHeader();
    }

}
