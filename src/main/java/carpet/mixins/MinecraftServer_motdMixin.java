package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(MinecraftServer.class)
public class MinecraftServer_motdMixin
{
    @Shadow @Nullable private String motd;

    @Redirect(method = "run", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/MinecraftServer;motd:Ljava/lang/String;")
    )
    private String setMotd(MinecraftServer server)
    {
        if ("_".equals(CarpetSettings.getString("customMOTD")))
            return motd;
        else
            return CarpetSettings.getString("customMOTD");
    }

}
