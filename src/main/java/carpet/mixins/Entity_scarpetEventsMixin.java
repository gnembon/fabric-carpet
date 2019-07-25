package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.EntityInterface;
import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.ScriptHost;
import carpet.settings.CarpetSettings;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class Entity_scarpetEventsMixin implements EntityInterface
{
    private CarpetEventServer.ScheduledCall deathCall;
    @Override
    public void onDeathCallback(String reason)
    {
        if (deathCall != null)
        {
            CarpetServer.scriptServer.events.onEntityDeath(deathCall, (Entity)(Object)this, reason);
        }
    }

    @Override
    public void setDeathCallback(CarpetContext cc, String function)
    {
        deathCall = CarpetServer.scriptServer.events.makeDeathCall(cc, function);
    }
}
