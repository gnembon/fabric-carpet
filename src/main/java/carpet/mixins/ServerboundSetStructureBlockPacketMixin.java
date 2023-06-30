package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundSetStructureBlockPacket.class)
public class ServerboundSetStructureBlockPacketMixin {
    @Mutable @Final @Shadow
    private BlockPos offset;
    @Mutable @Final @Shadow
    private Vec3i size;

    @Inject(
            method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At("TAIL")
    )
    private void structureBlockLimitsRead(FriendlyByteBuf buf, CallbackInfo ci) {
        if (buf.readableBytes() == 6*4) {
            // This will throw an exception if carpet is not installed on client
            offset = new BlockPos(Mth.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), Mth.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit), Mth.clamp(buf.readInt(), -CarpetSettings.structureBlockLimit, CarpetSettings.structureBlockLimit));
            size = new Vec3i(Mth.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), Mth.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit), Mth.clamp(buf.readInt(), 0, CarpetSettings.structureBlockLimit));
        }
    }

    @Inject(
            method = "write",
            at = @At("TAIL")
    )
    private void structureBlockLimitsWrite(FriendlyByteBuf buf, CallbackInfo ci) {
        //client method, only applicable if with carpet is on the server, or running locally
        if (CarpetSettings.structureBlockLimit != StructureBlockEntity.MAX_SIZE_PER_AXIS)
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
