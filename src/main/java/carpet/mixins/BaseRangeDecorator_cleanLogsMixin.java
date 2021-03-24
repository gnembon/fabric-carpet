package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.gen.decorator.BaseRangeDecorator;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseRangeDecorator.class)
public class BaseRangeDecorator_cleanLogsMixin {
    @Redirect(method = "getY(Lnet/minecraft/world/gen/decorator/DecoratorContext;Ljava/util/Random;Lnet/minecraft/world/gen/decorator/RangeDecoratorConfig;I)I",
     at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
    )
    private void skipLogs(Logger logger, String message, Object p0, Object p1, Object p2)
    {
        if (!CarpetSettings.cleanLogs) logger.warn(message, p0, p1, p2);
    }
}
