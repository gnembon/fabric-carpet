package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetExtension;
import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.settings.ParsedRule;
import carpet.settings.SettingsManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ClientNetworkHandler
{
    private static final Map<String, BiConsumer<ClientPlayerEntity, NbtElement>> dataHandlers = new HashMap<String, BiConsumer<ClientPlayerEntity, NbtElement>>();
    static
    {
        dataHandlers.put("Rules", (p, t) -> {
            NbtCompound ruleset = (NbtCompound)t;
            for (String ruleKey: ruleset.getKeys())
            {
                NbtCompound ruleNBT = (NbtCompound) ruleset.get(ruleKey);
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
                            SettingsManager eManager = extension.customSettingsManager();
                            if (eManager != null && managerName.equals(eManager.getIdentifier()))
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
                ParsedRule<?> rule = (manager != null) ? manager.getRule(ruleName) : null;
                if (rule != null)
                {
                    String value = ruleNBT.getString("Value");
                    try { rule.set(null, value); } catch (Exception ignored) { }
                }
            }
        });
        dataHandlers.put("TickRate", (p, t) -> TickSpeed.tickrate(((AbstractNbtNumber)t).floatValue(), false));
        dataHandlers.put("TickingState", (p, t) -> {
            NbtCompound tickingState = (NbtCompound)t;
            TickSpeed.setFrozenState(tickingState.getBoolean("is_paused"), tickingState.getBoolean("deepFreeze"));
        });
        dataHandlers.put("SuperHotState", (p, t) -> {
            TickSpeed.is_superHot = ((NbtByte) t).equals(NbtByte.ONE);
        });
        dataHandlers.put("TickPlayerActiveTimeout", (p, t) -> TickSpeed.player_active_timeout = ((AbstractNbtNumber)t).intValue());
        dataHandlers.put("scShape", (p, t) -> { // deprecated // and unused // should remove for 1.17
            if (CarpetClient.shapes != null)
                CarpetClient.shapes.addShape((NbtCompound)t);
        });
        dataHandlers.put("scShapes", (p, t) -> {
            if (CarpetClient.shapes != null)
                CarpetClient.shapes.addShapes((NbtList) t);
        });
        dataHandlers.put("clientCommand", (p, t) -> {
            CarpetClient.onClientCommand(t);
        });
    };

    public static void handleData(PacketByteBuf data, ClientPlayerEntity player)
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

    private static void onHi(PacketByteBuf data)
    {
        synchronized (CarpetClient.sync)
        {
            CarpetClient.setCarpet();
            CarpetClient.serverCarpetVersion = data.readString(64);
            if (CarpetSettings.carpetVersion.equals(CarpetClient.serverCarpetVersion))
            {
                CarpetSettings.LOG.info("Joined carpet server with matching carpet version");
            }
            else
            {
                CarpetSettings.LOG.warn("Joined carpet server with another carpet version: "+CarpetClient.serverCarpetVersion);
            }
            if (CarpetClient.getPlayer() != null)
                respondHello();

        }
    }

    public static void respondHello()
    {
        CarpetClient.getPlayer().networkHandler.sendPacket(new CustomPayloadC2SPacket(
                CarpetClient.CARPET_CHANNEL,
                (new PacketByteBuf(Unpooled.buffer())).writeVarInt(CarpetClient.HELLO).writeString(CarpetSettings.carpetVersion)
        ));
    }

    private static void onSyncData(PacketByteBuf data, ClientPlayerEntity player)
    {
        NbtCompound compound = data.readNbt();
        if (compound == null) return;
        for (String key: compound.getKeys())
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
        NbtCompound tag = new NbtCompound();
        tag.putString("id", command);
        tag.putString("command", command);
        NbtCompound outer = new NbtCompound();
        outer.put("clientCommand", tag);
        CarpetClient.getPlayer().networkHandler.sendPacket(new CustomPayloadC2SPacket(
                CarpetClient.CARPET_CHANNEL,
                (new PacketByteBuf(Unpooled.buffer())).writeVarInt(CarpetClient.DATA).writeNbt(outer)
        ));
    }
}
