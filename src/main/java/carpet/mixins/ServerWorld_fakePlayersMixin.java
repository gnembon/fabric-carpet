package carpet.mixins;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_fakePlayersMixin
{
    @Shadow private boolean ticking;

    @Shadow /*@Nonnull*/ public abstract MinecraftServer getServer();

    @Redirect( method = "removePlayer", at  = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;removeEntity(Lnet/minecraft/entity/Entity;)V"
    ))
    private void crashRemovePlayer(ServerWorld serverWorld, Entity entity_1, ServerPlayerEntity serverPlayerEntity_1)
    {
        if ( !(ticking && serverPlayerEntity_1 instanceof EntityPlayerMPFake) )
            serverWorld.removeEntity(entity_1);
        else
            getServer().method_18858(new ServerTask(getServer().getTicks(), () ->
            {
                serverWorld.removeEntity(serverPlayerEntity_1);
                serverPlayerEntity_1.onTeleportationDone();
            }));

    }
}
