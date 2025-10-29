package carpet.mixins;

import carpet.CarpetSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_parrotMixin extends Player {

    @Shadow
    public abstract CompoundTag getShoulderEntityLeft();

    @Shadow protected abstract void setShoulderEntityLeft(CompoundTag NbtCompound_1);

    @Shadow protected abstract void setShoulderEntityRight(CompoundTag NbtCompound_1);

    @Shadow public abstract CompoundTag getShoulderEntityRight();

    @Shadow protected abstract void respawnEntityOnShoulder(CompoundTag entityNbt);

    public ServerPlayer_parrotMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }


    protected void dismount_left()
    {
        respawnEntityOnShoulder(this.getShoulderEntityLeft());
        this.setShoulderEntityLeft(new CompoundTag());
    }

    protected void dismount_right()
    {
        respawnEntityOnShoulder(this.getShoulderEntityRight());
        this.setShoulderEntityRight(new CompoundTag());
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void onDamage(ServerLevel serverLevel, DamageSource damageSource_1, float float_1, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.persistentParrots && !this.isShiftKeyDown())
        {
            if (this.random.nextFloat() < ((float_1)/15.0) )
            {
                this.dismount_left();
            }
            if (this.random.nextFloat() < ((float_1)/15.0) )
            {
                this.dismount_right();
            }
        }
    }


    @Redirect(method = "handleShoulderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;removeEntitiesOnShoulder()V"))
    private void onRespawnParrotsInstead(ServerPlayer serverPlayer)
    {
        if (!CarpetSettings.persistentParrots)
        {
            this.removeEntitiesOnShoulder();
            return;
        }
        if (((getAbilities().invulnerable && this.fallDistance > 0.5F) || this.isInWater() || this.getAbilities().flying || isSleeping() || isInPowderSnow))
        {
            this.removeEntitiesOnShoulder();
        }
    }
}
