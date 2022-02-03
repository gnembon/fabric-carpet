package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Commands.class)
public abstract class Commands_customCommandsMixin
{

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRegister(Commands.CommandSelection arg, CallbackInfo ci) {
        CarpetServer.registerCarpetCommands(this.dispatcher);
        CarpetServer.registerCarpetCommands(this.dispatcher, arg);
    }

    @Inject(method = "performCommand", at = @At("HEAD"))
    private void onExecuteBegin(CommandSourceStack serverCommandSource_1, String string_1, CallbackInfoReturnable<Integer> cir)
    {
        if (!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "performCommand", at = @At("RETURN"))
    private void onExecuteEnd(CommandSourceStack serverCommandSource_1, String string_1, CallbackInfoReturnable<Integer> cir)
    {
        CarpetSettings.impendingFillSkipUpdates.set(false);
    }

    @ModifyExpressionValue(method = "performCommand", at = @At(
                value = "INVOKE",
                target = "Lorg/slf4j/Logger;isDebugEnabled()Z",
                remap = false
    ))
    private boolean doesOutputCommandStackTrace(boolean original)
    {
        return original || CarpetSettings.superSecretSetting;
    }
}