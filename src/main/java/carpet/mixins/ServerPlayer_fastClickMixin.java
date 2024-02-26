package carpet.mixins;

import carpet.fakes.ServerPlayerFastClickInterface;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_fastClickMixin extends Player implements ServerPlayerFastClickInterface
{
    public ServerPlayer_fastClickMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile)
    {
        super(level, blockPos, f, gameProfile);
    }

    private Vec3 posLastTick;
    private Vec2 rotLastTick;

    public void saveOldPosRot(Vec3 pos, Vec2 rot)
    {
        posLastTick = pos;
        rotLastTick = rot;
    }

    public void swapOldPosRot(boolean lastTickValues)
    {
        if (lastTickValues) {
            xo = posLastTick.x;
            yo = posLastTick.y;
            zo = posLastTick.z;
            xRotO = rotLastTick.x;
            yRotO = rotLastTick.y;
        } else {
            xo = getX();
            yo = getY();
            zo = getZ();
            xRotO = getXRot();
            yRotO = getYRot();
        }
    }
}

