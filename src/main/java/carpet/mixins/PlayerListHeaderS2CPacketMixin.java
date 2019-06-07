package carpet.mixins;

import net.minecraft.client.network.packet.PlayerListHeaderS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHeaderS2CPacket.class)
public interface PlayerListHeaderS2CPacketMixin
{
    @Accessor("header")
    void setHeader(Text header);

    @Accessor("footer")
    void setFooter(Text footer);
}