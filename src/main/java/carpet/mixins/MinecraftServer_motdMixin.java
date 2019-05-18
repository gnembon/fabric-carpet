package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.MinecraftServer_motdInterface;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_motdMixin implements MinecraftServer_motdInterface
{
    @Shadow private String motd;

    @Shadow public abstract void setMotd(String string_1);

    @Redirect(method = "run", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/MinecraftServer;motd:Ljava/lang/String;")
    )
    private String setCustomMotd(MinecraftServer server)
    {
        return checkMOTD();
    }

    @Override
    public String checkMOTD()
    {
        if ("_".equals(CarpetSettings.getString("customMOTD")))
        {
            setMotd(motd);
            return motd;
        }
        else
        {
            setMotd(CarpetSettings.getString("customMOTD"));
            return CarpetSettings.getString("customMOTD");
        }
    }
}
