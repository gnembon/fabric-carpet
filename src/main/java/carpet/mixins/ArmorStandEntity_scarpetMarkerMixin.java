package carpet.mixins;

import carpet.CarpetServer;
import carpet.script.ExpressionInspector;
import carpet.CarpetSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntity_scarpetMarkerMixin extends LivingEntity
{
    protected ArmorStandEntity_scarpetMarkerMixin(EntityType<? extends LivingEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    /**
     * Remove all markers that do not belong to any script host and not part of the global one when loaded
     * @param ci
     */
    @Inject(method = "readCustomDataFromTag", at = @At("HEAD"))
    private void checkScarpetMarkerUnloaded(CallbackInfo ci)
    {
        if ((CarpetSettings.scriptsAutoload || CarpetSettings.commandScript) && !world.isClient)
        {
            if (getScoreboardTags().contains(ExpressionInspector.MARKER_STRING))
            {
                String prefix = ExpressionInspector.MARKER_STRING+"_";
                Optional<String> owner = getScoreboardTags().stream().filter(s -> s.startsWith(prefix)).findFirst();
                if (owner.isPresent())
                {
                    String hostName = StringUtils.removeStart(owner.get(),prefix);
                    if (!hostName.isEmpty() && CarpetServer.scriptServer.getHostByName(hostName) == null)
                    {
                        remove();
                    }

                }
                else
                {
                    remove();
                }
            }
        }
    }
}
