package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.settings.ParsedRule;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ServerNetworkHandler
{
    private static List<ServerPlayerEntity> carpetPlayers = new ArrayList<>();

    public static void handleData(PacketByteBuf data, ServerPlayerEntity player)
    {
        if (data != null)
        {
            int id = data.readVarInt();
            if (id == CarpetClient.HELLO)
                onHello(player, data);
        }
    }
    public static void onPlayerJoin(ServerPlayerEntity playerEntity)
    {
        if (!playerEntity.networkHandler.connection.isLocal())
        {
            playerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    (new PacketByteBuf(Unpooled.buffer())).writeVarInt(CarpetClient.HI).writeString(CarpetSettings.carpetVersion)
            ));
        }
    }

    public static void onHello(ServerPlayerEntity playerEntity, PacketByteBuf packetData)
    {
        carpetPlayers.add(playerEntity);
        String clientVersion = packetData.readString(64);
        if (clientVersion.equals(CarpetSettings.carpetVersion))
            CarpetSettings.LOG.info("Player "+playerEntity.getName().getString()+" joined with a matching carpet client");
        else
            CarpetSettings.LOG.warn("Player "+playerEntity.getName().getString()+" joined with another carpet version: "+clientVersion);
        DataBuilder data = DataBuilder.create().withTickRate();
        CarpetServer.settingsManager.getRules().forEach(data::withRule);
        playerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetClient.CARPET_CHANNEL, data.build() ));
    }
    
    public static void updateRuleWithConnectedClients(ParsedRule<?> rule)
    {
        for (ServerPlayerEntity player : carpetPlayers)
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withRule(rule).build()
            ));
        }
    }
    
    public static void updateTickSpeedToConnectedPlayers()
    {
        for (ServerPlayerEntity player : carpetPlayers)
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withTickRate().build()
            ));
        }
    }

    public static void onPlayerLoggedOut(ServerPlayerEntity player)
    {
        if (!player.networkHandler.connection.isLocal())
            carpetPlayers.remove(player);
    }

    public static void close()
    {
        carpetPlayers.clear();
    }

    private static class DataBuilder
    {
        private CompoundTag tag;
        private static DataBuilder create()
        {
            return new DataBuilder();
        }
        private DataBuilder()
        {
            tag = new CompoundTag();
        }
        private DataBuilder withTickRate()
        {
            tag.putFloat("TickRate", TickSpeed.tickrate);
            return this;
        }
        private DataBuilder withRule(ParsedRule<?> rule)
        {
            CompoundTag rules = (CompoundTag) tag.get("Rules");
            if (rules == null)
            {
                rules = new CompoundTag();
                tag.put("Rules", rules);
            }
            CompoundTag ruleNBT = new CompoundTag();
            ruleNBT.putString("Value", rule.getAsString());
            rules.put(rule.name, ruleNBT);
            return this;
        }
        private PacketByteBuf build()
        {
            PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
            packetBuf.writeVarInt(CarpetClient.DATA);
            packetBuf.writeCompoundTag(tag);
            return packetBuf;
        }
    }
}
