package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalForcer.class)
public class PortalForcer_portalSuffocationMixin
{
    @Inject(method = "usePortal", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getLastNetherPortalDirectionVector()Lnet/minecraft/util/math/Vec3d;"
    ))
    private void registerEntityDimensionChange(Entity entity, float f, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.portalSuffocationFix)
        {
            CarpetSettings.currentTelepotingEntityBox = entity.getBoundingBox();
            CarpetSettings.fixedPosition = null;
        }
    }

    @Redirect(method = "usePortal", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;positAfterTeleport(DDD)V"
    ))
    private void alternativeSetPositionAndAngles(Entity entity, double d, double e, double f)
    {
        if (CarpetSettings.portalSuffocationFix && CarpetSettings.fixedPosition != null)
            entity.positAfterTeleport(CarpetSettings.fixedPosition.x, CarpetSettings.fixedPosition.y, CarpetSettings.fixedPosition.z);
        else
            entity.positAfterTeleport(d, e, f);

    }

    @Inject(method = "usePortal", at = @At("RETURN"))
    private void removeEntity(Entity entity_1, float float_1, CallbackInfoReturnable<Boolean> cir)
    {
        CarpetSettings.currentTelepotingEntityBox = null;
        CarpetSettings.fixedPosition = null;
    }


    @Redirect(method = "method_22391",at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/pattern/BlockPattern$Result;getTeleportTarget(Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;DLnet/minecraft/util/math/Vec3d;D)Lnet/minecraft/block/pattern/BlockPattern$TeleportTarget;"
    ))
    private BlockPattern.TeleportTarget newResult(BlockPattern.Result portal, Direction direction_1, BlockPos blockPos_1, double height_position_from_top, Vec3d vec3d_1, double width_position)
    {
        if (CarpetSettings.portalSuffocationFix && CarpetSettings.currentTelepotingEntityBox != null)
        {
            double entityWidth = CarpetSettings.currentTelepotingEntityBox.getXLength();
            double entityHeight = CarpetSettings.currentTelepotingEntityBox.getYLength();
            if (entityWidth >= portal.getWidth())
            {
                width_position = 0.5; // will suffocate anyways, placing in the middle
            }
            else
            {
                width_position = MathHelper.clamp(width_position, entityWidth/(2*portal.getWidth()), 1.0-entityWidth/(2*portal.getWidth()));
            }
            if (entityHeight >= portal.getHeight())
            {
                height_position_from_top = 1.0;
            }
            else
            {
                height_position_from_top = MathHelper.clamp(height_position_from_top, entityHeight/portal.getHeight(),1.0);
            }

        }
        BlockPattern.TeleportTarget target = portal.getTeleportTarget(direction_1, blockPos_1, height_position_from_top, vec3d_1, width_position);
        CarpetSettings.fixedPosition = target.pos;
        return target;
    }
}
