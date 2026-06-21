package carpet.mixins;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_fakeLastMovementMixin extends Player {
    public ServerPlayer_fakeLastMovementMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
        throw new AssertionError();
    }

    @ModifyExpressionValue(
        method = {"getKnownMovement", "getKnownSpeed"}, // both because ServerPlayer overrides both to the same "movement" field
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;lastKnownClientMovement:Lnet/minecraft/world/phys/Vec3;", opcode = Opcodes.GETFIELD),
        require = 2
    )
    private Vec3 bypassClientMovementInfo(Vec3 original) {
        return ((Player)this) instanceof EntityPlayerMPFake ? super.getKnownMovement() : original;
    }
}
