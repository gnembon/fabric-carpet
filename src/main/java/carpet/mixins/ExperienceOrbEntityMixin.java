package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity
{
    @Shadow
    private int pickingCount;

    @Shadow
    private int amount;

    public ExperienceOrbEntityMixin(EntityType<?> type, World world)
    {
        super(type, world);
    }

    @Shadow
    protected abstract int repairPlayerGears(PlayerEntity player, int amount);

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void addXP(PlayerEntity player, CallbackInfo ci) {
        if (CarpetSettings.xpNoCooldown && !world.isClient) {
            player.experiencePickUpDelay = 0;
            // reducing the count to 1 and leaving vanilla to deal with it
            while (this.pickingCount > 1) {
                int remainder = this.repairPlayerGears(player, this.amount);
                if (remainder > 0) {
                    player.addExperience(remainder);
                }
                this.pickingCount--;
            }
        }
    }
}
