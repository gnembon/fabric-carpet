package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.helpers.PortalHelper;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin
{
    @ModifyConstant(method = "getMaxPortalTime",
            constant = @Constant(intValue = 1))
    private int addFillUpdatesInt(int original) {
        if (CarpetSettings.portalCreativeDelay)
            if (PortalHelper.player_holds_obsidian((PlayerEntity) (Object)this))
                return 72000;
            else
                return 80;
        return original;
    }
}
