package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.command.CloneCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CloneCommand.class)
public abstract class CloneCommandMixin
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
