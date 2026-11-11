package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.fakes.CommandsInterface;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBox;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ParseBox;
import net.minecraft.commands.ParseContext;
import net.minecraft.commands.SuggestionBox;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class Commands_customCommandsMixin implements CommandsInterface
{
    @Shadow
    @Final
    private ParseBox parseBox;

    @Shadow
    @Final
    private SuggestionBox suggestionBox;

    @Unique
    private CommandDispatcher<CommandSourceStack> carpet$dispatcher;

    @ModifyArg(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/SuggestionBox;fromFullTree(Lcom/mojang/brigadier/CommandDispatcher;)Lnet/minecraft/commands/SuggestionBox;"
    ), index = 0)
    private CommandDispatcher<CommandSourceStack> captureDispatcher(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        carpet$dispatcher = dispatcher;
        return dispatcher;
    }

    @Override
    public CommandDispatcher<CommandSourceStack> carpet$getDispatcher()
    {
        return carpet$dispatcher;
    }

    @Override
    public void carpet$refreshCommandTrees()
    {
        ParseBox updatedParser = ParseBox.fromFullTree(carpet$dispatcher);
        SuggestionBox updatedSuggestions = SuggestionBox.fromFullTree(carpet$dispatcher);
        carpet$copyDispatcher(parseBox, updatedParser);
        carpet$copyDispatcher(suggestionBox, updatedSuggestions);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <C extends ParseContext> void carpet$copyDispatcher(CommandBox<C> target, CommandBox<C> source)
    {
        ((CommandBoxAccessor<C>) target).carpet$setDispatcher(((CommandBoxAccessor<C>) source).carpet$getDispatcher());
    }

    @Inject(method = "registerVanillaCommands", at = @At("RETURN"))
    private static void onRegister(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection commandSelection, CommandBuildContext commandBuildContext, CallbackInfo ci)
    {
        CarpetServer.registerCarpetCommands(dispatcher, commandSelection, commandBuildContext);
    }

    @Inject(method = "performCommand", at = @At("HEAD"))
    private static void onExecuteBegin(CommandSourceStack source, ParseBox.Result parseResults, String string, CallbackInfo ci)
    {
        if (!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "performCommand", at = @At("RETURN"))
    private static void onExecuteEnd(CommandSourceStack source, ParseBox.Result parseResults, String string, CallbackInfo ci)
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
    private static boolean doesOutputCommandStackTrace(Logger logger)
    {
        if (CarpetSettings.superSecretSetting)
            return true;
        return logger.isDebugEnabled();
    }
}
