package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.ThrowableSuppression;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntity_updateSuppressionCrashFixMixin {
    @Redirect(method = "playerTick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;tick()V"))
    private void fixUpdateSuppressionCrashPlayerTick(PlayerEntity playerEntity){
        if (!CarpetSettings.updateSuppressionCrashFix) {
            playerEntity.tick();
            return;
        }
        try {
            playerEntity.tick();
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
