package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.WorldInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_maxCollisionsMixin extends Entity
{

    public LivingEntity_maxCollisionsMixin(EntityType<?> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Shadow protected abstract void pushAway(Entity entity_1);

    @Inject(method = "tickCramming", cancellable = true, at = @At("HEAD"))
    private void tickPushingReplacement(CallbackInfo ci) {
        List<Entity> entities;
        int maxEntityCramming =-1;
        if (CarpetSettings.maxEntityCollisions > 0)
        {
            maxEntityCramming = this.world.getGameRules().getInt(GameRules.MAX_ENTITY_CRAMMING);
            entities = ((WorldInterface) this.world).getOtherEntitiesLimited(
                    this,
                    this.getBoundingBox(),
                    EntityPredicates.canBePushedBy(this),
                    Math.max(CarpetSettings.maxEntityCollisions, maxEntityCramming));
        }
        else
        {
            entities = this.world.getOtherEntities(this, this.getBoundingBox(), EntityPredicates.canBePushedBy(this));
        }

        if (!entities.isEmpty()) {
            if (maxEntityCramming < 0) maxEntityCramming = this.world.getGameRules().getInt(GameRules.MAX_ENTITY_CRAMMING);
            if (maxEntityCramming > 0 && entities.size() > maxEntityCramming - 1 && this.random.nextInt(4) == 0) {
                int candidates = 0;

                for (Entity entity : entities) {
                    if (!entity.hasVehicle()) {
                        ++candidates;
                    }
                }

                if (candidates > maxEntityCramming - 1) {
                    this.damage(DamageSource.CRAMMING, 6.0F);
                }
            }
            if (CarpetSettings.maxEntityCollisions > 0 && entities.size() > CarpetSettings.maxEntityCollisions)
            {
                for (Entity entity : entities.subList(0, CarpetSettings.maxEntityCollisions))
                {
                    this.pushAway(entity);
                }
            }
            else
            {
                for (Entity entity : entities)
                {
                    this.pushAway(entity);
                }
            }
        }
        ci.cancel();
    }


}
