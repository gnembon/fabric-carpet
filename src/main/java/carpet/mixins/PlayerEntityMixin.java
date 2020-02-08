package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow
    public PlayerAbilities abilities;

    public int getMaxNetherPortalTime() {
        return this.abilities.invulnerable ? CarpetSettings.portalCreativeDelay : CarpetSettings.portalSurvivalDelay;
    }
}
