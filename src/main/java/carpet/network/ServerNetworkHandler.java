package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.settings.ParsedRule;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.PacketByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ServerNetworkHandler
{
    private static Map<ServerPlayerEntity, String> remoteCarpetPlayers = new HashMap<>();
    private static Set<ServerPlayerEntity> validCarpetPlayers = new HashSet<>();

    private static Map<String, BiConsumer<ServerPlayerEntity, Tag>> dataHandlers = new HashMap<String, BiConsumer<ServerPlayerEntity, Tag>>(){{
        put("clientCommand", (p, t) -> {
            handleClientCommand(p, (CompoundTag)t);
        });
    }};

    public static void handleData(PacketByteBuf data, ServerPlayerEntity player)
    {
        if (data != null)
        {
            int id = data.readVarInt();
            if (id == CarpetClient.HELLO)
                onHello(player, data);
            if (id == CarpetClient.DATA)
                onClientData(player, data);
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
        else
        {
            validCarpetPlayers.add(playerEntity);
        }

    }

    public static void onHello(ServerPlayerEntity playerEntity, PacketByteBuf packetData)
    {

        validCarpetPlayers.add(playerEntity);
        String clientVersion = packetData.readString(64);
        remoteCarpetPlayers.put(playerEntity, clientVersion);
        if (clientVersion.equals(CarpetSettings.carpetVersion))
            CarpetSettings.LOG.info("Player "+playerEntity.getName().getString()+" joined with a matching carpet client");
        else
            CarpetSettings.LOG.warn("Player "+playerEntity.getName().getString()+" joined with another carpet version: "+clientVersion);
        DataBuilder data = DataBuilder.create().withTickRate();
        CarpetServer.settingsManager.getRules().forEach(data::withRule);
        playerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetClient.CARPET_CHANNEL, data.build() ));
    }

    private static void handleClientCommand(ServerPlayerEntity player, CompoundTag commandData)
    {
        String command = commandData.getString("command");
        String id = commandData.getString("id");
        List<Text> output = new ArrayList<>();
        String[] error = {null};
        int resultCode = -1;
        if (player.getServer() == null)
        {
            error[0] = "No Server";
        }
        else
        {
            resultCode = player.getServer().getCommandManager().execute(
                    new ServerCommandSource(player, player.getPos(), player.getRotationClient(),
                    player.world instanceof ServerWorld ? (ServerWorld) player.world : null,
                    player.server.getPermissionLevel(player.getGameProfile()), player.getName().getString(), player.getDisplayName(),
                    player.world.getServer(), player)
                    {
                        @Override
                        public void sendError(Text message)
                        {
                            error[0] = message.getString();
                        }
                        @Override
                        public void sendFeedback(Text message, boolean broadcastToOps)
                        {
                            output.add(message);
                        }
                    },
                    command
            );
        };
        CompoundTag result = new CompoundTag();
        result.putString("id", id);
        result.putInt("code", resultCode);
        if (error[0] != null) result.putString("error", error[0]);
        ListTag outputResult = new ListTag();
        for (Text line: output) outputResult.add(StringTag.of(Text.Serializer.toJson(line)));
        if (!output.isEmpty()) result.put("output", outputResult);
        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                CarpetClient.CARPET_CHANNEL,
                DataBuilder.create().withCustomNbt("clientCommand", result).build()
        ));
        // run command plug to command output,
    }


    private static void onClientData(ServerPlayerEntity player, PacketByteBuf data)
    {
        CompoundTag compound = data.readCompoundTag();
        if (compound == null) return;
        for (String key: compound.getKeys())
        {
            if (dataHandlers.containsKey(key))
                dataHandlers.get(key).accept(player, compound.get(key));
            else
                CarpetSettings.LOG.warn("Unknown carpet client data: "+key);
        }
    }
    
    public static void updateRuleWithConnectedClients(ParsedRule<?> rule)
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayerEntity player : remoteCarpetPlayers.keySet())
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withRule(rule).build()
            ));
        }
    }
    
    public static void updateTickSpeedToConnectedPlayers()
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayerEntity player : remoteCarpetPlayers.keySet())
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withTickRate().build()
            ));
        }
    }

    public static void broadcastCustomCommand(String command, Tag data)
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayerEntity player : validCarpetPlayers)
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withCustomNbt(command, data).build()
            ));
        }
    }

    public static void sendCustomCommand(ServerPlayerEntity player, String command, Tag data)
    {
        if (isValidCarpetPlayer(player))
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withCustomNbt(command, data).build()
            ));
        }
    }


    public static void onPlayerLoggedOut(ServerPlayerEntity player)
    {
        validCarpetPlayers.remove(player);
        if (!player.networkHandler.connection.isLocal())
            remoteCarpetPlayers.remove(player);
    }

    public static void close()
    {
        remoteCarpetPlayers.clear();
        validCarpetPlayers.clear();
    }

    public static boolean isValidCarpetPlayer(ServerPlayerEntity player)
    {
        if (CarpetSettings.superSecretSetting) return false;
        return validCarpetPlayers.contains(player);

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

        public DataBuilder withCustomNbt(String key, Tag value)
        {
            tag.put(key, value);
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
