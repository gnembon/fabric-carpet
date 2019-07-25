package carpet.fakes;

import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.ScriptHost;
import carpet.script.value.Value;

import java.util.List;

public interface EntityInterface
{
    float getMainYaw(float partialTicks);
    //scarpet part
    void onDeathCallback(String reason);
    void setDeathCallback(CarpetContext cc, String function, List<Value> extraArgs);
    void onDamageCallback();
    void setDamageCallback(CarpetContext cc, String function, List<Value> extraArgs);
    void onRemovedCallback();
    void setRemovedCallback(CarpetContext cc, String function, List<Value> extraArgs);
}
