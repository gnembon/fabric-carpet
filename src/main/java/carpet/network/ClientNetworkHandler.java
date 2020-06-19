package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.settings.ParsedRule;
import io.netty.buffer.Unpooled;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ClientNetworkHandler
{
    private static Map<String, BiConsumer<ClientPlayerEntity, Tag>> dataHandlers = new HashMap<String, BiConsumer<ClientPlayerEntity, Tag>>(){{
        put("Rules", (p, t) -> {
            CompoundTag ruleset = (CompoundTag)t;
            for (String ruleName: ruleset.getKeys())
            {
                ParsedRule<?> rule = CarpetServer.settingsManager.getRule(ruleName);
                if (rule == null)
                    CarpetSettings.LOG.error("Received unknown rule: "+ruleName);
                else
                {
                    CompoundTag ruleNBT = (CompoundTag) ruleset.get(ruleName);
                    String value = ruleNBT.getString("Value");
                    rule.set(null, value);
                }
            }
        });
        put("TickRate", (p, t) -> TickSpeed.tickrate(((AbstractNumberTag)t).getFloat(), false));
        put("scShape", (p, t) -> { // deprecated
            if (CarpetClient.shapes != null)
                CarpetClient.shapes.addShape((CompoundTag)t);
        });
        put("scShapes", (p, t) -> {
            if (CarpetClient.shapes != null)
                CarpetClient.shapes.addShapes((ListTag) t);
        });
        put("clientCommand", (p, t) -> {
            CarpetClient.onClientCommand(t);
        });
    }};


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
        CompoundTag compound = data.readCompoundTag();
        if (compound == null) return;
        for (String key: compound.getKeys())
        {
            if (dataHandlers.containsKey(key))
                dataHandlers.get(key).accept(player, compound.get(key));
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
        CarpetClient.getPlayer().networkHandler.sendPacket(new CustomPayloadC2SPacket(
                CarpetClient.CARPET_CHANNEL,
                (new PacketByteBuf(Unpooled.buffer())).writeVarInt(CarpetClient.DATA).writeCompoundTag(outer)
        ));
    }
}
