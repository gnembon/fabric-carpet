package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Commands.class)
public abstract class Commands_customCommandsMixin
{

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRegister(Commands.CommandSelection commandSelection, CommandBuildContext commandBuildContext, CallbackInfo ci) {
        CarpetServer.registerCarpetCommands(this.dispatcher, commandSelection, commandBuildContext);
    }

    @Inject(method = "performCommand", at = @At("HEAD"))
    private void onExecuteBegin(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfoReturnable<Integer> cir)
    {
        if (!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "performCommand", at = @At("RETURN"))
    private void onExecuteEnd(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfoReturnable<Integer> cir)
    {
        CarpetSettings.impendingFillSkipUpdates.set(false);
    }

    @Redirect(method = "performCommand", at = @At(
                value = "INVOKE",
                target = "Lorg/slf4j/Logger;isDebugEnabled()Z",
                remap = false
            ),
        require = 0
    )
    private boolean doesOutputCommandStackTrace(Logger logger)
    {
        if (CarpetSettings.superSecretSetting)
            return true;
        return logger.isDebugEnabled();
    }
}
