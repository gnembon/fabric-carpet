package carpet.mixins;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SoundEngine.class, priority = 69420)
public class SoundEngine_cleanLogsMixin
{
    /*@Redirect(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", require = 0, at = @At( remap = false,
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V"
    ))
    private void toWarnOrNotToWarn(Logger logger, String message)
    {
        if (!CarpetSettings.cleanLogs) logger.warn(message);
    }*/
}
