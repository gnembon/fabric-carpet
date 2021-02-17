package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.network.CarpetClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UpdateStructureBlockC2SPacket.class)
public class UpdateStructureBlockC2SPacketMixin {
    @Shadow
    private BlockPos offset;
    @Shadow
    private BlockPos size;

    @Inject(
            method = "read",
            at = @At("TAIL")
    )
    private void structureBlockLimitsRead(PacketByteBuf buf, CallbackInfo ci) {
        // fabric-carpet client 1.4.25 ~ 1.4.26 compatibility
        // TODO remove at some point with a major MC release as not important
        if (buf.readableBytes() == 6*4) {
            // This will throw an exception if carpet is not installed on client
            offset = new BlockPos(MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit));
            size = new BlockPos(MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit));
        }
    }
}
