package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.fakes.LivingEntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_TAKES_DAMAGE;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_scarpetEventsMixin extends Entity implements LivingEntityInterface {

    @Shadow
    protected abstract void jumpFromGround();

    @Shadow
    protected boolean jumping;

    public LivingEntity_scarpetEventsMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow
    public abstract int getArmorValue();

    @Shadow
    public abstract double getAttributeValue(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute);

    @Shadow
    public abstract boolean hasEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect);

    @Shadow
    public abstract MobEffectInstance getEffect(
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect);

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeathCall(DamageSource damageSource_1, CallbackInfo ci) {
        ((EntityInterface) this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DEATH,
                damageSource_1.getMsgId());
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void entityTakingDamage(ServerLevel serverLevel, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        float predictedDamage = amount;
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            float armor = (float) this.getArmorValue();
            float toughness = (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            predictedDamage = CombatRules.getDamageAfterAbsorb(
                    (LivingEntity) (Object) this, predictedDamage, source, armor, toughness);
        }
        if (!source.is(DamageTypeTags.BYPASSES_EFFECTS)
                && this.hasEffect(MobEffects.RESISTANCE)
                && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int resistanceLevel = this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1;
            predictedDamage = CombatRules.getDamageAfterMagicAbsorb(
                    predictedDamage, (float) (resistanceLevel * 5));
        }

        ((EntityInterface) this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DAMAGE, predictedDamage,
                source);

        if (((Object) this) instanceof ServerPlayer && PLAYER_TAKES_DAMAGE.isNeeded()) {
            if (PLAYER_TAKES_DAMAGE.onDamage((LivingEntity) (Object) this, predictedDamage, source)) {
                cir.setReturnValue(false);
                return;
            }
        }

        if (source.getEntity() instanceof ServerPlayer && PLAYER_DEALS_DAMAGE.isNeeded()) {
            if (PLAYER_DEALS_DAMAGE.onDamage((LivingEntity) (Object) this, predictedDamage, source)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Override
    public void doJumpCM() {
        jumpFromGround();
    }

    @Override
    public boolean isJumpingCM() {
        return jumping;
    }
}
