package carpet.mixins;

import carpet.settings.CarpetSettings;
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

    @Inject(method = "tickPushing", cancellable = true, at = @At("HEAD"))
    private void tickPushingReplacement(CallbackInfo ci)
    {
        List<Entity> list_1 = this.world.getEntities((Entity)this, this.getBoundingBox(), EntityPredicates.canBePushedBy(this));
        if (!list_1.isEmpty()) {
            int int_1 = this.world.getGameRules().getInt(GameRules.MAX_ENTITY_CRAMMING);
            int int_2;
            if (int_1 > 0 && list_1.size() > int_1 - 1 && this.random.nextInt(4) == 0) {
                int_2 = 0;

                for(int int_3 = 0; int_3 < list_1.size(); ++int_3) {
                    if (!((Entity)list_1.get(int_3)).hasVehicle()) {
                        ++int_2;
                    }
                }

                if (int_2 > int_1 - 1) {
                    this.damage(DamageSource.CRAMMING, 6.0F);
                }
            }

            int limit = list_1.size();
            if (CarpetSettings.maxEntityCollisions > 0)
                limit = Math.min(limit, CarpetSettings.maxEntityCollisions);

            for(int_2 = 0; int_2 < limit; ++int_2) {
                Entity entity_1 = (Entity)list_1.get(int_2);
                this.pushAway(entity_1);
            }
        }
        ci.cancel();
    }


}
