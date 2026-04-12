package carpet.mixins;

import carpet.CarpetSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class Player_parrotMixin extends LivingEntity
{
    @Shadow protected abstract void removeEntitiesOnShoulder();

    protected Player_parrotMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"))
    private void cancelDropShoulderEntities2(Player playerEntity)
    {
        if (!CarpetSettings.persistentParrots) {
            removeEntitiesOnShoulder();
        }
    }
    


}
