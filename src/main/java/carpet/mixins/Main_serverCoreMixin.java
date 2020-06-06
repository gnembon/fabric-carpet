package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class Main_serverCoreMixin
{
    @Inject(method = "main", at = @At( // little bit later than main to add chance for extensions to register.
            value = "INVOKE",
            target = "Ljoptsimple/OptionParser;parse([Ljava/lang/String;)Ljoptsimple/OptionSet;")
    )
    private static void onServerStarted(String[] args, CallbackInfo ci)
    {
        CarpetServer.onGameStarted();
    }
}
