package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_parrotMixin extends LivingEntity
{
    @Shadow @Final public PlayerAbilities abilities;
    
    @Shadow protected abstract void dropShoulderEntities();
    
    @Shadow protected abstract void method_7296(CompoundTag compoundTag_1);
    
    @Shadow public abstract CompoundTag getShoulderEntityLeft();
    
    @Shadow protected abstract void setShoulderEntityLeft(CompoundTag compoundTag_1);
    
    @Shadow protected abstract void setShoulderEntityRight(CompoundTag compoundTag_1);
    
    @Shadow public abstract CompoundTag getShoulderEntityRight();
    
    protected PlayerEntity_parrotMixin(EntityType<? extends LivingEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }
    
    @Redirect(method = "tickMovement", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void cancelDropShoulderEntities1(PlayerEntity playerEntity)
    {
    
    }
    
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 1,
            target = "Lnet/minecraft/entity/player/PlayerEntity;updateShoulderEntity(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void onTickMovement(CallbackInfo ci)
    {
        boolean parrots_will_drop = !CarpetSettings.persistentParrots || this.abilities.invulnerable;
        if (!this.world.isClient && ((parrots_will_drop && this.fallDistance > 0.5F) || this.isInWater() || (parrots_will_drop && this.hasVehicle())) || this.abilities.flying)
        {
            this.dropShoulderEntities();
        }
    }
    
    @Redirect(method = "damage", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void cancelDropShoulderEntities2(PlayerEntity playerEntity)
    {
    
    }
    
    protected void dismount_left()
    {
        this.method_7296(this.getShoulderEntityLeft());
        this.setShoulderEntityLeft(new CompoundTag());
    }
    
    protected void dismount_right()
    {
        this.method_7296(this.getShoulderEntityRight());
        this.setShoulderEntityRight(new CompoundTag());
    }
    
    @Inject(method = "damage", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void onDamage(DamageSource damageSource_1, float float_1, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.persistentParrots && !this.isSneaking())
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
