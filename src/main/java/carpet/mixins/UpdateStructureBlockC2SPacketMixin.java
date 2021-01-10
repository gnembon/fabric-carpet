package carpet.mixins;

import carpet.CarpetSettings;
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
        BlockPos previousOffset = offset;
        BlockPos previousSize = size;
        try {
            // This will throw an exception if carpet is not installed on client
            offset = new BlockPos(MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit));
            size = new BlockPos(MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), MathHelper.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit));
        } catch (Exception exception) {
            offset = previousOffset;
            size = previousSize;
        }
    }

    @Inject(
            method = "write",
            at = @At("TAIL")
    )
    private void structureBlockLimitsWrite(PacketByteBuf buf, CallbackInfo ci) {
        buf.writeInt(this.offset.getX());
        buf.writeInt(this.offset.getY());
        buf.writeInt(this.offset.getZ());
        buf.writeInt(this.size.getX());
        buf.writeInt(this.size.getY());
        buf.writeInt(this.size.getZ());
    }
}
