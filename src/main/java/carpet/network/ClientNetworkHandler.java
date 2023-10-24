package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetExtension;
import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import carpet.api.settings.SettingsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public class ClientNetworkHandler
{
    private static final Map<String, BiConsumer<LocalPlayer, Tag>> dataHandlers = new HashMap<String, BiConsumer<LocalPlayer, Tag>>();

    static
    {
        dataHandlers.put(CarpetClient.HI, (p, t) -> onHi(t.getAsString()));
        dataHandlers.put("Rules", (p, t) -> {
            CompoundTag ruleset = (CompoundTag) t;
            for (String ruleKey : ruleset.getAllKeys())
            {
                CompoundTag ruleNBT = (CompoundTag) ruleset.get(ruleKey);
                SettingsManager manager = null;
                String ruleName;
                if (ruleNBT.contains("Manager"))
                {
                    ruleName = ruleNBT.getString("Rule");
                    String managerName = ruleNBT.getString("Manager");
                    if (managerName.equals("carpet"))
                    {
                        manager = CarpetServer.settingsManager;
                    }
                    else
                    {
                        for (CarpetExtension extension : CarpetServer.extensions)
                        {
                            SettingsManager eManager = extension.extensionSettingsManager();
                            if (eManager != null && managerName.equals(eManager.identifier()))
                            {
                                manager = eManager;
                                break;
                            }
                        }
                    }
                }
                else // Backwards compatibility
                {
                    manager = CarpetServer.settingsManager;
                    ruleName = ruleKey;
                }
                CarpetRule<?> rule = (manager != null) ? manager.getCarpetRule(ruleName) : null;
                if (rule != null)
                {
                    String value = ruleNBT.getString("Value");
                    try
                    {
                        rule.set(null, value);
                    }
                    catch (InvalidRuleValueException ignored)
                    {
                    }
                }
            }
        });
        dataHandlers.put("scShape", (p, t) -> { // deprecated // and unused // should remove for 1.17
            if (CarpetClient.shapes != null)
            {
                CarpetClient.shapes.addShape((CompoundTag) t);
            }
        });
        dataHandlers.put("scShapes", (p, t) -> {
            if (CarpetClient.shapes != null)
            {
                CarpetClient.shapes.addShapes((ListTag) t);
            }
        });
        dataHandlers.put("clientCommand", (p, t) -> CarpetClient.onClientCommand(t));
    }

    // Ran on the Main Minecraft Thread

    private static void onHi(String version)
    {
        CarpetClient.setCarpet();
        CarpetClient.serverCarpetVersion = version;
        if (CarpetSettings.carpetVersion.equals(CarpetClient.serverCarpetVersion))
        {
            CarpetSettings.LOG.info("Joined carpet server with matching carpet version");
        }
        else
        {
            CarpetSettings.LOG.warn("Joined carpet server with another carpet version: " + CarpetClient.serverCarpetVersion);
        }
        // We can ensure that this packet is
        // processed AFTER the player has joined
        respondHello();
    }

    public static void respondHello()
    {
        CompoundTag data = new CompoundTag();
        data.putString(CarpetClient.HELLO, CarpetSettings.carpetVersion);
        CarpetClient.getPlayer().connection.send(new ServerboundCustomPayloadPacket(
                new CarpetClient.CarpetPayload(data)
        ));
    }

    public static void onServerData(CompoundTag compound, LocalPlayer player)
    {
        for (String key : compound.getAllKeys())
        {
            if (dataHandlers.containsKey(key))
            {
                try
                {
                    dataHandlers.get(key).accept(player, compound.get(key));
                }
                catch (Exception exc)
                {
                    CarpetSettings.LOG.info("Corrupt carpet data for " + key);
                }
            }
            else
            {
                CarpetSettings.LOG.error("Unknown carpet data: " + key);
            }
        }
    }

    public static void clientCommand(String command)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", command);
        tag.putString("command", command);
        CompoundTag outer = new CompoundTag();
        outer.put("clientCommand", tag);
        CarpetClient.getPlayer().connection.send(new ServerboundCustomPayloadPacket(
                new CarpetClient.CarpetPayload(outer)
        ));
    }
}
