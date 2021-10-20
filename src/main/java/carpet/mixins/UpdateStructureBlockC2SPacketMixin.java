package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.network.CarpetClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UpdateStructureBlockC2SPacket.class)
public class UpdateStructureBlockC2SPacketMixin {
    @Mutable @Final @Shadow
    private BlockPos offset;
    @Mutable @Final @Shadow
    private Vec3i size;

    @Inject(
            method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V",
            at = @At("TAIL")
    )
    private void structureBlockLimitsRead(PacketByteBuf buf, CallbackInfo ci) {
        if (buf.readableBytes() == 6*4) {
            // This will throw an exception if carpet is not installed on client
            offset = new BlockPos(MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit));
            size = new Vec3i(MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit));
        }
    }

    @Inject(
            method = "write",
            at = @At("TAIL")
    )
    private void structureBlockLimitsWrite(PacketByteBuf buf, CallbackInfo ci) {
        //client method, only applicable if with carpet is on the server, or running locally
        if (CarpetSettings.structureBlockLimit != CarpetSettings.vanillaStructureBlockLimit)
        {
            buf.writeInt(this.offset.getX());
            buf.writeInt(this.offset.getY());
            buf.writeInt(this.offset.getZ());
            buf.writeInt(this.size.getX());
            buf.writeInt(this.size.getY());
            buf.writeInt(this.size.getZ());
        }
    }
}
