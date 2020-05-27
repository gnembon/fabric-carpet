package carpet.helpers;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.settings.ParsedRule;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;

public class ServerNetworkHandler
{
    public static void onPlayerJoin(ServerPlayerEntity playerEntity)
    {
        CompoundTag data = new CompoundTag();
        
        data.putFloat("Tickrate", TickSpeed.tickrate);
        
        ListTag rulesList = new ListTag();
        for (ParsedRule<?> rule : CarpetServer.settingsManager.getRules())
        {
            CompoundTag ruleNBT = new CompoundTag();
            
            ruleNBT.putString("rule" ,rule.name);
            ruleNBT.putString("value", rule.getAsString());
            ruleNBT.putString("default", rule.defaultAsString);
            rulesList.add(ruleNBT);
        }
        data.put("rules", rulesList);
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        packetBuf.writeVarInt(1);
        packetBuf.writeCompoundTag(data);
        playerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetServer.CARPET_CHANNEL, packetBuf));
    }
    
    public static void sendUpdateToClient(String rule, String newValue, ServerCommandSource source)
    {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        
        data.writeVarInt(2);
        data.writeString(rule);
        data.writeString(newValue);
        for (ServerPlayerEntity player : source.getMinecraftServer().getPlayerManager().getPlayerList())
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetServer.CARPET_CHANNEL, data));
        }
    }
    
    public static void sendTickRateToPlayers(float rate)
    {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeVarInt(3);
        data.writeFloat(rate);
        if (CarpetServer.minecraft_server != null)
        {
            for (ServerPlayerEntity player : CarpetServer.minecraft_server.getPlayerManager().getPlayerList())
            {
                player.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetServer.CARPET_CHANNEL, data));
            }
        }
    }
}
