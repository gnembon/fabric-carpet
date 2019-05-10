package carpet.mixins;

import carpet.CarpetServer;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
{
    // Called during game start
    @Inject(method = "<init>", at = @At(value = "HEAD"))
    private void onMinecraftServerCTOR(File file_1, Proxy proxy_1, DataFixer dataFixer_1,
                                       CommandManager serverCommandManager_1, YggdrasilAuthenticationService yggdrasilAuthenticationService_1,
                                       MinecraftSessionService minecraftSessionService_1, GameProfileRepository gameProfileRepository_1,
                                       UserCache userCache_1, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory_1,
                                       String string_1, CallbackInfo ci)
    {
        CarpetServer.init((MinecraftServer) (Object) this);
    }

    //to inject right before
    // this.tickWorlds(booleanSupplier_1);
    @Inject(
            method = "Lnet/minecraft/server/MinecraftServer;tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.BEFORE, ordinal = 0)
    )
    private void onTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetServer.tick((MinecraftServer) (Object) this);
    }
}
