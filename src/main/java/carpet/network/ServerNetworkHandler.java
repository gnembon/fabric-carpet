package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.script.utils.SnoopyCommandSource;
import carpet.settings.ParsedRule;
import carpet.settings.SettingsManager;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

public class ServerNetworkHandler
{
    private static Map<ServerPlayer, String> remoteCarpetPlayers = new HashMap<>();
    private static Set<ServerPlayer> validCarpetPlayers = new HashSet<>();

    private static Map<String, BiConsumer<ServerPlayer, Tag>> dataHandlers = new HashMap<String, BiConsumer<ServerPlayer, Tag>>(){{
        put("clientCommand", (p, t) -> {
            handleClientCommand(p, (CompoundTag)t);
        });
    }};

    public static void handleData(FriendlyByteBuf data, ServerPlayer player)
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



    public static void onPlayerJoin(ServerPlayer playerEntity)
    {
        if (!playerEntity.connection.connection.isMemoryConnection())
        {
            playerEntity.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    (new FriendlyByteBuf(Unpooled.buffer())).writeVarInt(CarpetClient.HI).writeUtf(CarpetSettings.carpetVersion)
            ));
        }
        else
        {
            validCarpetPlayers.add(playerEntity);
        }

    }

    public static void onHello(ServerPlayer playerEntity, FriendlyByteBuf packetData)
    {

        validCarpetPlayers.add(playerEntity);
        String clientVersion = packetData.readUtf(64);
        remoteCarpetPlayers.put(playerEntity, clientVersion);
        if (clientVersion.equals(CarpetSettings.carpetVersion))
            CarpetSettings.LOG.info("Player "+playerEntity.getName().getString()+" joined with a matching carpet client");
        else
            CarpetSettings.LOG.warn("Player "+playerEntity.getName().getString()+" joined with another carpet version: "+clientVersion);
        DataBuilder data = DataBuilder.create().withTickRate().withFrozenState().withTickPlayerActiveTimeout(); // .withSuperHotState()
        CarpetServer.settingsManager.getRules().forEach(data::withRule);
        CarpetServer.extensions.forEach(e -> {
            SettingsManager eManager = e.customSettingsManager();
            if (eManager != null) {
                eManager.getRules().forEach(data::withRule);
            }
        });
        playerEntity.connection.send(new ClientboundCustomPayloadPacket(CarpetClient.CARPET_CHANNEL, data.build() ));
    }

    private static void handleClientCommand(ServerPlayer player, CompoundTag commandData)
    {
        String command = commandData.getString("command");
        String id = commandData.getString("id");
        List<Component> output = new ArrayList<>();
        Component[] error = {null};
        int resultCode = -1;
        if (player.getServer() == null)
        {
            error[0] = new TextComponent("No Server");
        }
        else
        {
            resultCode = player.getServer().getCommands().performCommand(
                    new SnoopyCommandSource(player, error, output), command
            );
        }
        CompoundTag result = new CompoundTag();
        result.putString("id", id);
        result.putInt("code", resultCode);
        if (error[0] != null) result.putString("error", error[0].getContents());
        ListTag outputResult = new ListTag();
        for (Component line: output) outputResult.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        if (!output.isEmpty()) result.put("output", outputResult);
        player.connection.send(new ClientboundCustomPayloadPacket(
                CarpetClient.CARPET_CHANNEL,
                DataBuilder.create().withCustomNbt("clientCommand", result).build()
        ));
        // run command plug to command output,
    }


    private static void onClientData(ServerPlayer player, FriendlyByteBuf data)
    {
        CompoundTag compound = data.readNbt();
        if (compound == null) return;
        for (String key: compound.getAllKeys())
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
        for (ServerPlayer player : remoteCarpetPlayers.keySet())
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withRule(rule).build()
            ));
        }
    }
    
    public static void updateTickSpeedToConnectedPlayers()
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayer player : remoteCarpetPlayers.keySet())
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withTickRate().build()
            ));
        }
    }

    public static void updateFrozenStateToConnectedPlayers()
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayer player : remoteCarpetPlayers.keySet())
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withFrozenState().build()
            ));
        }
    }

    public static void updateSuperHotStateToConnectedPlayers()
    {
        if(CarpetSettings.superSecretSetting) return;
        for (ServerPlayer player : remoteCarpetPlayers.keySet())
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withSuperHotState().build()
            ));
        }
    }

    public static void updateTickPlayerActiveTimeoutToConnectedPlayers()
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayer player : remoteCarpetPlayers.keySet())
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withTickPlayerActiveTimeout().build()
            ));
        }
    }

    public static void broadcastCustomCommand(String command, Tag data)
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayer player : validCarpetPlayers)
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withCustomNbt(command, data).build()
            ));
        }
    }

    public static void sendCustomCommand(ServerPlayer player, String command, Tag data)
    {
        if (isValidCarpetPlayer(player))
        {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withCustomNbt(command, data).build()
            ));
        }
    }


    public static void onPlayerLoggedOut(ServerPlayer player)
    {
        validCarpetPlayers.remove(player);
        if (!player.connection.connection.isMemoryConnection())
            remoteCarpetPlayers.remove(player);
    }

    public static void close()
    {
        remoteCarpetPlayers.clear();
        validCarpetPlayers.clear();
    }

    public static boolean isValidCarpetPlayer(ServerPlayer player)
    {
        if (CarpetSettings.superSecretSetting) return false;
        return validCarpetPlayers.contains(player);

    }

    public static String getPlayerStatus(ServerPlayer player)
    {
        if (remoteCarpetPlayers.containsKey(player)) return "carpet "+remoteCarpetPlayers.get(player);
        if (validCarpetPlayers.contains(player)) return "carpet "+CarpetSettings.carpetVersion;
        return "vanilla";
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
        private DataBuilder withFrozenState()
        {
            CompoundTag tickingState = new CompoundTag();
            tickingState.putBoolean("is_paused", TickSpeed.isPaused());
            tickingState.putBoolean("deepFreeze", TickSpeed.deeplyFrozen());
            tag.put("TickingState", tickingState);
            return this;
        }
        private DataBuilder withSuperHotState()
        {
        	tag.putBoolean("SuperHotState", TickSpeed.is_superHot);
        	return this;
        }
        private DataBuilder withTickPlayerActiveTimeout()
        {
            tag.putInt("TickPlayerActiveTimeout", TickSpeed.player_active_timeout);
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
            String identifier = rule.settingsManager.getIdentifier();
            String key = rule.name;
            while (rules.contains(key)) { key = key+"2";}
            CompoundTag ruleNBT = new CompoundTag();
            ruleNBT.putString("Value", rule.getAsString());
            ruleNBT.putString("Manager",identifier);
            ruleNBT.putString("Rule",rule.name);
            rules.put(key, ruleNBT);
            return this;
        }

        public DataBuilder withCustomNbt(String key, Tag value)
        {
            tag.put(key, value);
            return this;
        }

        private FriendlyByteBuf build()
        {
            FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
            packetBuf.writeVarInt(CarpetClient.DATA);
            packetBuf.writeNbt(tag);
            return packetBuf;
        }


    }
}
