package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_xpNoCooldownMixin {

    @Shadow
    protected abstract void collideWithEntity(Entity entity);

    @Redirect(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "java/util/List.add(Ljava/lang/Object;)Z"
            )
    )
    public boolean processXpOrbCollisions(List<Entity> instance, Object e) {
        Entity entity = (Entity) e;
        // 431 and 17 have no special meaning, just random numbers
        if (CarpetSettings.xpNoCooldown && (entity.getId() % 17 == 0 || entity.age % 431 == 0)) {
            this.collideWithEntity(entity);
            return true;
        }
        return instance.add(entity);
    }
}
