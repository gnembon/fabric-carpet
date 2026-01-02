package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.Set;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin_itemRotation {


    @Inject(
            method = "getPortalDestination(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/portal/TeleportTransition;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void carpetDebug$fixItemRotateDelta(ServerLevel world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir) {
        System.out.println("Hello; ");
        System.out.println(entity);

        if (CarpetSettings.endPortalItemVelocityRotationFix && entity instanceof ItemEntity) {
            TeleportTransition original = cir.getReturnValue();
            if (original != null) {
                // Make a mutable set (immutable -> mutable); EnumSet is ideal for enum flags.
                Set<Relative> flags = original.relatives();
                Set<Relative> mutable = flags.isEmpty() ? EnumSet.noneOf(Relative.class) : EnumSet.copyOf(flags);

                // Remove the flag safely
                mutable.remove(Relative.ROTATE_DELTA);

                TeleportTransition patched = new TeleportTransition(
                        original.newLevel(),
                        original.position(),
                        original.deltaMovement(),
                        original.yRot(),
                        original.xRot(),
                        mutable,
                        original.postTeleportTransition()
                );
                cir.setReturnValue(patched);
            }
        }
    }
}
