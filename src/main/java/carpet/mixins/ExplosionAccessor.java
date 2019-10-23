package carpet.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Random;

@Mixin(Explosion.class)
public interface ExplosionAccessor {

    @Accessor
    boolean isCreateFire();

    @Accessor
    Explosion.DestructionType getBlockDestructionType();

    @Accessor
    World getWorld();

    @Accessor
    Random getRandom();

    @Accessor
    double getX();

    @Accessor
    double getY();

    @Accessor
    double getZ();

    @Accessor
    float getPower();

    @Accessor
    Entity getEntity();

}
