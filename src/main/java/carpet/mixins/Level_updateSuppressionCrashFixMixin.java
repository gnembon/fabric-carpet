package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.ThrowableSuppression;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Level.class)
public class Level_updateSuppressionCrashFixMixin {

    @Inject(
            method = "neighborChanged",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"
            ),
            locals =  LocalCapture.CAPTURE_FAILHARD,
            require = 0
    )
    public void checkUpdateSuppression(BlockPos sourcePos, Block sourceBlock, BlockPos neighborPos, CallbackInfo ci, BlockState state,Throwable throwable){
        if(CarpetSettings.updateSuppressionCrashFix && (throwable instanceof ThrowableSuppression || throwable instanceof StackOverflowError)){
            throw new ThrowableSuppression("Update suppression",neighborPos);
        }
    }
}
