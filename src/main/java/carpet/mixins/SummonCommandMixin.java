package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.TranslatableText;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.SummonCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SummonCommand.class)
public class SummonCommandMixin
{
    @Inject(method = "execute", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;addLightning(Lnet/minecraft/entity/LightningEntity;)V",
            shift = At.Shift.BEFORE
    ))
    private static void addRiders(ServerCommandSource source, Identifier identifier_1, Vec3d vec3d_1, CompoundTag compoundTag_1, boolean arg4, CallbackInfoReturnable<Integer> cir, CompoundTag compoundTag_2, LightningEntity lightningEntity_1)
    {
        // [CM] SummonNaturalLightning - if statement around
        if (CarpetSettings.summonNaturalLightning && !source.getWorld().isClient)
        {
            ServerWorld world = source.getWorld();
            BlockPos at = new BlockPos(vec3d_1);
            LocalDifficulty localDifficulty_1 =  source.getWorld().getLocalDifficulty(at);
            boolean boolean_2 = world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING) && world.random.nextDouble() < (double)localDifficulty_1.getLocalDifficulty() * 0.01D;
            if (boolean_2) {
                SkeletonHorseEntity skeletonHorseEntity_1 = (SkeletonHorseEntity) EntityType.SKELETON_HORSE.create(world);
                skeletonHorseEntity_1.setTrapped(true);
                skeletonHorseEntity_1.setBreedingAge(0);
                skeletonHorseEntity_1.setPosition(vec3d_1.x, vec3d_1.y, vec3d_1.z);
                world.spawnEntity(skeletonHorseEntity_1);
            }
            world.addLightning(lightningEntity_1);
            source.sendFeedback(new TranslatableText("commands.summon.success", new Object[]{lightningEntity_1.getDisplayName()}), true);
            cir.setReturnValue(1);
            cir.cancel();

        }
    }

}
