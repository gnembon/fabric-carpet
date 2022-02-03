package carpet.mixins;

import carpet.fakes.PlayerListHudInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(Gui.class)
public abstract class Gui_tablistMixin
{
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow @Final private PlayerTabOverlay tabList;

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean onDraw(boolean original)
    {
        return original && !((PlayerListHudInterface) tabList).hasFooterOrHeader();
    }

}
