package carpet.mixins;

import carpet.utils.CommandHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.WritableRegistry;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionCheckTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PermissionCheckTypes.class)
public class PermissionCheckTypes_carpetMixin
{
    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static void registerCarpetChecks(WritableRegistry<MapCodec<? extends PermissionCheck>> registry, CallbackInfo ci)
    {
        CommandHelper.registerPermissionCheckTypes(registry);
    }
}
