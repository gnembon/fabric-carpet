package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.LevelInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_maxCollisionsMixin extends Entity
{

    public LivingEntity_maxCollisionsMixin(EntityType<?> entityType_1, Level world_1)
    {
        super(entityType_1, world_1);
    }

    @Shadow protected abstract void doPush(Entity entity_1);

    @Inject(method = "pushEntities", cancellable = true, at = @At("HEAD"))
    private void tickPushingReplacement(CallbackInfo ci) {
        if (CarpetSettings.maxEntityCollisions == 0)
        {
            return;
        }
        List<Entity> entities;
        int maxEntityCramming =-1;
        if (CarpetSettings.maxEntityCollisions > 0)
        {
            maxEntityCramming = this.getServer().getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            entities = ((LevelInterface) this.level()).getOtherEntitiesLimited(
                    this,
                    this.getBoundingBox(),
                    EntitySelector.pushableBy(this),
                    Math.max(CarpetSettings.maxEntityCollisions, maxEntityCramming));
        }
        else
        {
            entities = this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
        }

        if (!entities.isEmpty()) {
            if (maxEntityCramming < 0) maxEntityCramming = this.getServer().getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            if (maxEntityCramming > 0 && entities.size() > maxEntityCramming - 1 && this.random.nextInt(4) == 0) {
                int candidates = 0;

                for (Entity entity : entities) {
                    if (!entity.isPassenger()) {
                        ++candidates;
                    }
                }

                if (candidates > maxEntityCramming - 1) {
                    this.hurt(damageSources().cramming(), 6.0F);
                }
            }
            if (CarpetSettings.maxEntityCollisions > 0 && entities.size() > CarpetSettings.maxEntityCollisions)
            {
                for (Entity entity : entities.subList(0, CarpetSettings.maxEntityCollisions))
                {
                    this.doPush(entity);
                }
            }
            else
            {
                for (Entity entity : entities)
                {
                    this.doPush(entity);
                }
            }
        }
        ci.cancel();
    }


}
