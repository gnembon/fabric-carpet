package carpet.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;

@Mixin(ServerExplosion.class)
public interface ExplosionAccessor {

    @Accessor
    ServerLevel getLevel();

    @Accessor
    Vec3 getCenter();

    @Accessor
    float getRadius();

    @Accessor
    Entity getSource();

    @Accessor
    DamageSource getDamageSource();

}
