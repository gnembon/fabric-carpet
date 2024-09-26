package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
    @Shadow @Final public Abilities abilities;
    
    @Shadow protected abstract void removeEntitiesOnShoulder();
    
    @Shadow public abstract CompoundTag getShoulderEntityLeft();
    
    @Shadow protected abstract void setShoulderEntityLeft(CompoundTag NbtCompound_1);
    
    @Shadow protected abstract void setShoulderEntityRight(CompoundTag NbtCompound_1);
    
    @Shadow public abstract CompoundTag getShoulderEntityRight();

    @Shadow protected abstract void respawnEntityOnShoulder(CompoundTag entityNbt);

    protected Player_parrotMixin(EntityType<? extends LivingEntity> entityType_1, Level world_1)
    {
        super(entityType_1, world_1);
    }
    
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"))
    private void cancelDropShoulderEntities1(Player playerEntity)
    {
    
    }
    
    @Inject(method = "aiStep", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 1,
            target = "Lnet/minecraft/world/entity/player/Player;playShoulderEntityAmbientSound(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void onTickMovement(CallbackInfo ci)
    {
        boolean parrots_will_drop = !CarpetSettings.persistentParrots || this.abilities.invulnerable;
        if (!this.level().isClientSide && ((parrots_will_drop && this.fallDistance > 0.5F) || this.isInWater() || this.abilities.flying || isSleeping()))
        {
            this.removeEntitiesOnShoulder();
        }
    }
    
    @Redirect(method = "hurtServer", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"))
    private void cancelDropShoulderEntities2(Player playerEntity)
    {
    
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
    
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"))
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
}
