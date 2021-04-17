package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.biome.source.BiomeArray;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeArray.class)
public class BiomeArray_cleanLogsMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/util/collection/IndexedIterable;[I)V", at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V")
    )
    private void skipLog(Logger logger, String message)
    {
        if (!CarpetSettings.cleanLogs) logger.warn(message);
    }
}
