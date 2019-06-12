package carpet.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugHud.class)
public class DebugHudMixin
{
    @Redirect(method = "getLeftText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getGameVersion()Ljava/lang/String;"
    ))
    private String getCarpetClient(MinecraftClient minecraftClient)
    {
        return "vanilla enough";
    }

    @Redirect(method = "getLeftText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/ClientBrandRetriever;getClientModName()Ljava/lang/String;"
    ))
    private String getModName()
    {
        return "don't even...";
    }

    @Redirect(method = "getLeftText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getVersionType()Ljava/lang/String;"
    ))
    private String getVersionType(MinecraftClient minecraftClient)
    {
        return "release";
    }

}
