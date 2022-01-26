package carpet.mixins;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundTabListPacket.class)
public interface ClientboundTabListPacketMixin
{
    @Accessor("header")
    void setHeader(Component header);

    @Accessor("footer")
    void setFooter(Component footer);
}