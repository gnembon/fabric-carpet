package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
{

    public LivingEntityMixin(EntityType<?> type, World world)
    {
        super(type, world);
    }

    @Redirect(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasCustomName()Z"))
    private boolean shouldLogDeaths(LivingEntity livingEntity)
    {
        return livingEntity.hasCustomName() && CarpetSettings.cleanLogs && world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES);
    }
}
