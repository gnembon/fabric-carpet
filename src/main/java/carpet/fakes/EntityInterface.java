package carpet.fakes;

import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.ScriptHost;

public interface EntityInterface
{
    float getMainYaw(float partialTicks);
    //scarpet part
    void onDeathCallback(String reason);
    void setDeathCallback(CarpetContext cc, String function);
    void onDamageCallback();
    void setDamageCallback(CarpetEventServer.ScheduledCall call);
}
