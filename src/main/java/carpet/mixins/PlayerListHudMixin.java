package carpet.mixins;
import carpet.fakes.PlayerListHudInterface;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin implements PlayerListHudInterface
{
    @Shadow private Component footer;

    @Shadow private Component header;

    public boolean hasFooterOrHeader()
    {
        return footer != null || header != null;
    }
}