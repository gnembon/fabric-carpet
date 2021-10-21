package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.ThrowableSuppression;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.BaseText;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServer_updateSuppressionCrashFixMixin {

    @Redirect(
            method = "tickWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V"
            ),
            require = 0
    )
    private void fixUpdateSuppressionCrashTick(ServerWorld serverWorld, BooleanSupplier shouldKeepTicking){
        if (!CarpetSettings.updateSuppressionCrashFix) {
            serverWorld.tick(shouldKeepTicking);
            return;
        }
        try {
            serverWorld.tick(shouldKeepTicking);
        } catch (CrashException e) {
            if (!(e.getCause() instanceof ThrowableSuppression throwableSuppression)) throw e;
            logUpdateSuppression(throwableSuppression.pos);
        } catch (ThrowableSuppression e) {
            logUpdateSuppression(e.pos);
        }
    }


    private void logUpdateSuppression(BlockPos pos) {
        if(LoggerRegistry.__updateSuppressedCrashes) {
            LoggerRegistry.getLogger("updateSuppressedCrashes").log(() -> {
                return new BaseText[]{Messenger.c(
                        "w Server crash prevented in: ",
                        "m world tick ",
                        "w - at: ",
                        "g [ " + pos.toShortString() + " ]"
                )};
            });
        }
    }
}
