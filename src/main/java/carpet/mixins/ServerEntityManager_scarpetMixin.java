package carpet.mixins;

import carpet.script.CarpetEventServer;
import carpet.script.CarpetScriptServer;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerEntityManager.class)
public class ServerEntityManager_scarpetMixin
{
    @Inject(method = "addEntity(Lnet/minecraft/world/entity/EntityLike;Z)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityTrackingStatus;shouldTick()Z"
    ))
    private void handleAddedEntity(EntityLike entityLike, boolean existing, CallbackInfoReturnable<Boolean> cir)
    {
        Entity entity = (Entity)entityLike;
        CarpetEventServer.Event event = CarpetEventServer.Event.ENTITY_LOAD.get(entity.getType());
        if (event != null)
        {
            if (event.isNeeded())
            {
                event.onEntityAction(entity, !existing);
            }
        }
        else
        {
            CarpetScriptServer.LOG.error("Failed to handle entity type " + entity.getType().getTranslationKey());
        }

    }

}
