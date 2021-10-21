package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.ThrowableSuppression;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntity_updateSuppressionCrashFixMixin extends PlayerEntity {

    public ServerPlayerEntity_updateSuppressionCrashFixMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Redirect(
            method = "playerTick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;tick()V"
            ),
            require = 0
    )
    private void fixUpdateSuppressionCrashPlayerTick(PlayerEntity playerEntity){
        if (!CarpetSettings.updateSuppressionCrashFix) {
            super.tick();
            return;
        }
        try {
            super.tick();
        } catch (CrashException e) {
            if (!(e.getCause() instanceof ThrowableSuppression throwableSuppression)) throw e;
            logUpdateSuppressionPlayer(throwableSuppression.pos);
        } catch (ThrowableSuppression e) {
            logUpdateSuppressionPlayer(e.pos);
        }
    }


    private void logUpdateSuppressionPlayer(BlockPos pos) {
        if(LoggerRegistry.__updateSuppressedCrashes) {
            LoggerRegistry.getLogger("updateSuppressedCrashes").log(() -> {
                return new BaseText[]{Messenger.c(
                        "w Server crash prevented in: ",
                        "m player tick ",
                        "w - at: ",
                        "g [ " + pos.toShortString() + " ]"
                )};
            });
        }
    }
}
