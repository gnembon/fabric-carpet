package carpet.mixins;

import carpet.CarpetServer;
import carpet.script.api.Auxiliary;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

@Mixin(ArmorStand.class)
public abstract class ArmorStand_scarpetMarkerMixin extends LivingEntity
{
    protected ArmorStand_scarpetMarkerMixin(EntityType<? extends LivingEntity> entityType_1, Level world_1)
    {
        super(entityType_1, world_1);
    }

    /**
     * Remove all markers that do not belong to any script host and not part of the global one when loaded
     * @param ci
     */
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void checkScarpetMarkerUnloaded(CallbackInfo ci)
    {
        if (!level().isClientSide)
        {
            if (getTags().contains(Auxiliary.MARKER_STRING))
            {
                String prefix = Auxiliary.MARKER_STRING+"_";
                Optional<String> owner = getTags().stream().filter(s -> s.startsWith(prefix)).findFirst();
                if (owner.isPresent())
                {
                    String hostName = StringUtils.removeStart(owner.get(),prefix);
                    if (!hostName.isEmpty() && CarpetServer.scriptServer.getAppHostByName(hostName) == null)
                    {
                        discard();  //discard
                    }

                }
                else
                {
                    discard(); // discard
                }
            }
        }
    }
}
