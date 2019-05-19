package carpet.mixins;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpawnRestriction.class)
public abstract class SpawnRestrictionMixin
{
    @Shadow
    private static void register(EntityType<?> a, SpawnRestriction.Location b, Heightmap.Type c) { }

    static {
        register(EntityType.SHULKER, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES);
    }
}