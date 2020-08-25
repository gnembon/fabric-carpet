package carpet.mixins;

import carpet.CarpetSettings;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets="net.minecraft.client.sound.SoundEngine$SourceSetImpl")
public class SoundEngineInt_cleanLogsMixin
{
    @Redirect(method = "createSource", at = @At(
            value="INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"
    ))
    private void doWarnOrNotWarn(Logger logger, String message, Object p0)
    {
        if (!CarpetSettings.cleanLogs) logger.warn(message, p0);
    }
}
