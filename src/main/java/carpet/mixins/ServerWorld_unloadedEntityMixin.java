package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public class ServerWorld_unloadedEntityMixin
{
    @Redirect(method =  "checkChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;teleportRequested()Z"
    ))
    private boolean addFastEntityCheck(Entity entity)
    {
        return CarpetSettings.unloadedEntityFix || entity.teleportRequested();
    }
}
