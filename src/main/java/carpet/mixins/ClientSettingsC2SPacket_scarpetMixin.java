package carpet.mixins;

import carpet.fakes.ClientSettingsC2SPacketInterface;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//@Mixin(ClientSettingsC2SPacket.class)
public class ClientSettingsC2SPacket_scarpetMixin implements ClientSettingsC2SPacketInterface {
   /* @Final
    @Shadow
    private String language;

    @Override
    public String getLanguage(){
        return this.language;
    }*/
}

