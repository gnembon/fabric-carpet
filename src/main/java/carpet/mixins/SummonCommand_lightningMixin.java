package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SummonCommand.class)
public class SummonCommand_lightningMixin
{
    @Redirect(method = "createEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"
    ))
    private static BlockPos addRiders(Entity entity)
    {
        // [CM] SummonNaturalLightning - if statement around
        if (CarpetSettings.summonNaturalLightning && entity instanceof LightningBolt && !entity.getCommandSenderWorld().isClientSide)
        {
            ServerLevel world = (ServerLevel) entity.getCommandSenderWorld();
            BlockPos at = entity.blockPosition();
            DifficultyInstance localDifficulty_1 =  world.getCurrentDifficultyAt(at);
            boolean boolean_2 = world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && world.random.nextDouble() < (double)localDifficulty_1.getEffectiveDifficulty() * 0.01D;
            if (boolean_2) {
                SkeletonHorse skeletonHorseEntity_1 = EntityType.SKELETON_HORSE.create(world, EntitySpawnReason.EVENT);
                skeletonHorseEntity_1.setTrap(true);
                skeletonHorseEntity_1.setAge(0);
                skeletonHorseEntity_1.setPos(entity.getX(), entity.getY(), entity.getZ());
                world.addFreshEntity(skeletonHorseEntity_1);
            }
        }
        return entity.blockPosition();
    }

}
