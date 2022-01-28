package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.ThrowableSuppression;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import com.mojang.authlib.GameProfile;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_updateSuppressionCrashFixMixin extends Player {

    public ServerPlayer_updateSuppressionCrashFixMixin(Level world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Redirect(
            method = "doTick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;tick()V"
            ),
            require = 0
    )
    private void fixUpdateSuppressionCrashPlayerTick(Player playerEntity){
        if (!CarpetSettings.updateSuppressionCrashFix) {
            super.tick();
            return;
        }
        try {
            super.tick();
        } catch (ReportedException e) {
            if (!(e.getCause() instanceof ThrowableSuppression throwableSuppression)) throw e;
            logUpdateSuppressionPlayer(throwableSuppression.pos);
        } catch (ThrowableSuppression e) {
            logUpdateSuppressionPlayer(e.pos);
        }
    }


    private void logUpdateSuppressionPlayer(BlockPos pos) {
        if(LoggerRegistry.__updateSuppressedCrashes) {
            LoggerRegistry.getLogger("updateSuppressedCrashes").log(() -> {
                return new BaseComponent[]{Messenger.c(
                        "w Server crash prevented in: ",
                        "m player tick ",
                        "w - at: ",
                        "g [ " + pos.toShortString() + " ]"
                )};
            });
        }
    }
}
