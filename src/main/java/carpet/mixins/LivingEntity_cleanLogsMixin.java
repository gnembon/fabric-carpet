package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_cleanLogsMixin extends Entity
{

    public LivingEntity_cleanLogsMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    @ModifyExpressionValue(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasCustomName()Z"))
    private boolean shouldLogDeaths(boolean original)
    {
        return original && CarpetSettings.cleanLogs && level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
    }
}
