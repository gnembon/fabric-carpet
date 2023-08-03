package carpet.mixins;
import carpet.fakes.PlayerTabOverlayInterface;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin implements PlayerTabOverlayInterface
{
    @Shadow private Component footer;

    @Shadow private Component header;

    @Override
    public boolean carpet$hasFooterOrHeader()
    {
        return footer != null || header != null;
    }
}