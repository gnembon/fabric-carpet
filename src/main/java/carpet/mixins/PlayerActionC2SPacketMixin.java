package carpet.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundPlayerActionPacket.class)
public interface PlayerActionC2SPacketMixin
{
    @Accessor("pos")
    void setPos(BlockPos pos);

    @Accessor("direction")
    void setDirection(Direction direction);

    @Accessor("action")
    void setAction(ServerboundPlayerActionPacket.Action action);
}
