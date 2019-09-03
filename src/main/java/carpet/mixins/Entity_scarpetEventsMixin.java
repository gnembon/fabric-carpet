package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.EntityInterface;
import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.ScriptHost;
import carpet.script.value.Value;
import carpet.settings.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class Entity_scarpetEventsMixin implements EntityInterface
{
    @Shadow public boolean removed;
    private CarpetEventServer.ScheduledCall deathCall;
    private CarpetEventServer.ScheduledCall removeCall;
    private CarpetEventServer.ScheduledCall tickCall;
    private CarpetEventServer.ScheduledCall damageCall;




    @Override
    public void setDeathCallback(CarpetContext cc, String function, List<Value> extraArgs)
    {
        deathCall = CarpetServer.scriptServer.events.makeDeathCall(cc, function, extraArgs);
    }

    @Override
    public void setRemovedCallback(CarpetContext cc, String function, List<Value> extraArgs)
    {
        removeCall = CarpetServer.scriptServer.events.makeRemovedCall(cc, function, extraArgs);
    }

    @Override
    public void setTickCallback(CarpetContext cc, String function, List<Value> extraArgs)
    {
        tickCall = CarpetServer.scriptServer.events.makeTickCall(cc, function, extraArgs);
    }

    @Override
    public void setDamageCallback(CarpetContext cc, String function, List<Value> extraArgs)
    {
        damageCall = CarpetServer.scriptServer.events.makeDamageCall(cc, function, extraArgs);
    }


    @Override
    public void onDeathCallback(String reason)
    {
        if (deathCall != null)
        {
            CarpetServer.scriptServer.events.onEntityDeath(deathCall, (Entity)(Object)this, reason);
        }
    }

    @Override
    public void onDamageCallback(float amount, DamageSource source)
    {
        if (damageCall != null)
        {
            CarpetServer.scriptServer.events.onEntityDamage(damageCall, (Entity)(Object)this, amount, source);
        }
    }


    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCall(CallbackInfo ci)
    {
        if (tickCall != null)
        {
            CarpetServer.scriptServer.events.onEntityTick(tickCall, (Entity) (Object) this);
        }
    }


    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(CallbackInfo ci)
    {
        if (!removed && removeCall != null)
        {
            CarpetServer.scriptServer.events.onEntityRemoved(removeCall, (Entity)(Object)this);
        }
    }
}
