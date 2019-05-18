package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.command.FillCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FillCommand.class)
public abstract class FillCommandMixin
{
    @Redirect(method = "execute", at = @At(
            value = "CONSTANT",
            args = "intValue=32768"
    ))
    private static int fillLimit()
    {
        return CarpetSettings.getInt("fillLimit");
    }
}
