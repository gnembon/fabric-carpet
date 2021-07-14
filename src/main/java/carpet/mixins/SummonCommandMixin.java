package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.server.command.SummonCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SummonCommand.class)
public class SummonCommandMixin
{
    @Redirect(method = "execute", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getBlockPos()Lnet/minecraft/util/math/BlockPos;"
    ))
    private static BlockPos addRiders(Entity entity)
    {
        // [CM] SummonNaturalLightning - if statement around
        if (CarpetSettings.summonNaturalLightning && entity instanceof LightningEntity && !entity.getEntityWorld().isClient)
        {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            BlockPos at = entity.getBlockPos();
            LocalDifficulty localDifficulty_1 =  world.getLocalDifficulty(at);
            boolean boolean_2 = world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING) && world.random.nextDouble() < (double)localDifficulty_1.getLocalDifficulty() * 0.01D;
            if (boolean_2) {
                SkeletonHorseEntity skeletonHorseEntity_1 = EntityType.SKELETON_HORSE.create(world);
                skeletonHorseEntity_1.setTrapped(true);
                skeletonHorseEntity_1.setBreedingAge(0);
                skeletonHorseEntity_1.setPosition(entity.getX(), entity.getY(), entity.getZ());
                world.spawnEntity(skeletonHorseEntity_1);
            }
        }
        return entity.getBlockPos();
    }

}
