package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetExtension;
import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import carpet.fakes.LevelInterface;
import carpet.helpers.TickRateManager;
import carpet.api.settings.SettingsManager;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class ClientNetworkHandler
{
    private static final Map<String, BiConsumer<LocalPlayer, Tag>> dataHandlers = new HashMap<String, BiConsumer<LocalPlayer, Tag>>();
    static
    {
        dataHandlers.put("Rules", (p, t) -> {
            CompoundTag ruleset = (CompoundTag)t;
            for (String ruleKey: ruleset.getAllKeys())
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
                        for (CarpetExtension extension: CarpetServer.extensions) {
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
                    try { rule.set(null, value); } catch (InvalidRuleValueException ignored) { }
                }
            }
        });
        dataHandlers.put("TickRate", (p, t) -> {
            TickRateManager tickRateManager = ((LevelInterface)p.clientLevel).tickRateManager();
            tickRateManager.setTickRate(((NumericTag) t).getAsFloat());
        });
        dataHandlers.put("TickingState", (p, t) -> {
            CompoundTag tickingState = (CompoundTag)t;
            TickRateManager tickRateManager = ((LevelInterface)p.clientLevel).tickRateManager();
            tickRateManager.setFrozenState(tickingState.getBoolean("is_paused"), tickingState.getBoolean("deepFreeze"));
        });
        dataHandlers.put("SuperHotState", (p, t) -> {
            TickRateManager tickRateManager = ((LevelInterface)p.clientLevel).tickRateManager();
            tickRateManager.setSuperHot(((ByteTag) t).equals(ByteTag.ONE));
        });
        dataHandlers.put("TickPlayerActiveTimeout", (p, t) -> {
            TickRateManager tickRateManager = ((LevelInterface)p.clientLevel).tickRateManager();
            tickRateManager.setPlayerActiveTimeout(((NumericTag) t).getAsInt());
        });
        dataHandlers.put("scShape", (p, t) -> { // deprecated // and unused // should remove for 1.17
            if (CarpetClient.shapes != null)
                CarpetClient.shapes.addShape((CompoundTag)t);
        });
        dataHandlers.put("scShapes", (p, t) -> {
            if (CarpetClient.shapes != null)
                CarpetClient.shapes.addShapes((ListTag) t);
        });
        dataHandlers.put("clientCommand", (p, t) -> {
            CarpetClient.onClientCommand(t);
        });
    };

    // Ran on the Main Minecraft Thread
    public static void handleData(FriendlyByteBuf data, LocalPlayer player)
    {
        if (data != null)
        {
            int id = data.readVarInt();
            if (id == CarpetClient.HI)
                onHi(data);
            if (id == CarpetClient.DATA)
                onSyncData(data, player);
        }
    }

    private static void onHi(FriendlyByteBuf data)
    {
        CarpetClient.setCarpet();
        CarpetClient.serverCarpetVersion = data.readUtf(64);
        if (CarpetSettings.carpetVersion.equals(CarpetClient.serverCarpetVersion))
        {
            CarpetSettings.LOG.info("Joined carpet server with matching carpet version");
        }
        else
        {
            CarpetSettings.LOG.warn("Joined carpet server with another carpet version: "+CarpetClient.serverCarpetVersion);
        }
        // We can ensure that this packet is
        // processed AFTER the player has joined
        respondHello();
    }

    public static void respondHello()
    {
        CarpetClient.getPlayer().connection.send(new ServerboundCustomPayloadPacket(
                new CustomPacketPayload()
                {
                    @Override
                    public void write(final FriendlyByteBuf friendlyByteBuf)
                    {
                        friendlyByteBuf.writeVarInt(CarpetClient.HELLO).writeUtf(CarpetSettings.carpetVersion);
                    }

                    @Override
                    public ResourceLocation id()
                    {
                        return CarpetClient.CARPET_CHANNEL;
                    }
                }
        ));
    }

    private static void onSyncData(FriendlyByteBuf data, LocalPlayer player)
    {
        CompoundTag compound = data.readNbt();
        if (compound == null) return;
        for (String key: compound.getAllKeys())
        {
            if (dataHandlers.containsKey(key)) {
                try {
                    dataHandlers.get(key).accept(player, compound.get(key));
                }
                catch (Exception exc)
                {
                    CarpetSettings.LOG.info("Corrupt carpet data for "+key);
                }
            }
            else
                CarpetSettings.LOG.error("Unknown carpet data: "+key);
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
                new CustomPacketPayload()
                {
                    @Override
                    public void write( FriendlyByteBuf friendlyByteBuf)
                    {
                        friendlyByteBuf.writeVarInt(CarpetClient.DATA).writeNbt(outer);
                    }

                    @Override
                    public ResourceLocation id()
                    {
                        return CarpetClient.CARPET_CHANNEL;
                    }
                }
        ));
    }
}
