package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_6342;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(class_6342.class)
public class BaseRangeDecorator2_cleanLogsMixin {

    //@SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "get",
            at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V")
    )
    private void skipLogs(Logger logger, String message, Object p0)
    {
        if (!CarpetSettings.cleanLogs) logger.warn(message, p0);
    }
}
