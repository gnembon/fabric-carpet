package carpet.mixins;

import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerActionC2SPacket.class)
public interface PlayerActionC2SPacketMixin
{
    @Accessor("pos")
    void setPos(BlockPos pos);

    @Accessor("direction")
    void setDirection(Direction direction);

    @Accessor("action")
    void setAction(PlayerActionC2SPacket.Action action);
}
