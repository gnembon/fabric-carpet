package carpet.fakes;

import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.ScriptHost;
import carpet.script.value.Value;
import net.minecraft.entity.damage.DamageSource;

import java.util.List;

public interface EntityInterface
{
    float getMainYaw(float partialTicks);
    //scarpet part
    void onDeathCallback(String reason);
    void setDeathCallback(CarpetContext cc, String function, List<Value> extraArgs);
    void onDamageCallback(float amount, DamageSource source);
    void setDamageCallback(CarpetContext cc, String function, List<Value> extraArgs);
    void setRemovedCallback(CarpetContext cc, String function, List<Value> extraArgs);
    void setTickCallback(CarpetContext cc, String function, List<Value> extraArgs);
}
