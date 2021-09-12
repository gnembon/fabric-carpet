package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.script.utils.SnoopyCommandSource;
import carpet.settings.CarpetRule;
import carpet.settings.RuleHelper;
import carpet.settings.SettingsManager;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

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

    private static Map<String, BiConsumer<ServerPlayerEntity, NbtElement>> dataHandlers = new HashMap<String, BiConsumer<ServerPlayerEntity, NbtElement>>(){{
        put("clientCommand", (p, t) -> {
            handleClientCommand(p, (NbtCompound)t);
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
        DataBuilder data = DataBuilder.create().withTickRate().withFrozenState().withTickPlayerActiveTimeout(); // .withSuperHotState()
        CarpetServer.settingsManager.getCarpetRules().forEach(data::withRule);
        CarpetServer.extensions.forEach(e -> {
            SettingsManager eManager = e.customSettingsManager();
            if (eManager != null) {
                eManager.getCarpetRules().forEach(data::withRule);
            }
        });
        playerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetClient.CARPET_CHANNEL, data.build() ));
    }

    private static void handleClientCommand(ServerPlayerEntity player, NbtCompound commandData)
    {
        String command = commandData.getString("command");
        String id = commandData.getString("id");
        List<Text> output = new ArrayList<>();
        Text[] error = {null};
        int resultCode = -1;
        if (player.getServer() == null)
        {
            error[0] = new LiteralText("No Server");
        }
        else
        {
            resultCode = player.getServer().getCommandManager().execute(
                    new SnoopyCommandSource(player, error, output), command
            );
        }
        NbtCompound result = new NbtCompound();
        result.putString("id", id);
        result.putInt("code", resultCode);
        if (error[0] != null) result.putString("error", error[0].asString());
        NbtList outputResult = new NbtList();
        for (Text line: output) outputResult.add(NbtString.of(Text.Serializer.toJson(line)));
        if (!output.isEmpty()) result.put("output", outputResult);
        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                CarpetClient.CARPET_CHANNEL,
                DataBuilder.create().withCustomNbt("clientCommand", result).build()
        ));
        // run command plug to command output,
    }


    private static void onClientData(ServerPlayerEntity player, PacketByteBuf data)
    {
        NbtCompound compound = data.readNbt();
        if (compound == null) return;
        for (String key: compound.getKeys())
        {
            if (dataHandlers.containsKey(key))
                dataHandlers.get(key).accept(player, compound.get(key));
            else
                CarpetSettings.LOG.warn("Unknown carpet client data: "+key);
        }
    }

    public static void updateRuleWithConnectedClients(CarpetRule<?> rule)
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

    public static void updateFrozenStateToConnectedPlayers()
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayerEntity player : remoteCarpetPlayers.keySet())
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withFrozenState().build()
            ));
        }
    }

    public static void updateSuperHotStateToConnectedPlayers()
    {
        if(CarpetSettings.superSecretSetting) return;
        for (ServerPlayerEntity player : remoteCarpetPlayers.keySet())
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withSuperHotState().build()
            ));
        }
    }

    public static void updateTickPlayerActiveTimeoutToConnectedPlayers()
    {
        if (CarpetSettings.superSecretSetting) return;
        for (ServerPlayerEntity player : remoteCarpetPlayers.keySet())
        {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    CarpetClient.CARPET_CHANNEL,
                    DataBuilder.create().withTickPlayerActiveTimeout().build()
            ));
        }
    }

    public static void broadcastCustomCommand(String command, NbtElement data)
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

    public static void sendCustomCommand(ServerPlayerEntity player, String command, NbtElement data)
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

    public static String getPlayerStatus(ServerPlayerEntity player)
    {
        if (remoteCarpetPlayers.containsKey(player)) return "carpet "+remoteCarpetPlayers.get(player);
        if (validCarpetPlayers.contains(player)) return "carpet "+CarpetSettings.carpetVersion;
        return "vanilla";
    }

    private static class DataBuilder
    {
        private NbtCompound tag;
        private static DataBuilder create()
        {
            return new DataBuilder();
        }
        private DataBuilder()
        {
            tag = new NbtCompound();
        }
        private DataBuilder withTickRate()
        {
            tag.putFloat("TickRate", TickSpeed.tickrate);
            return this;
        }
        private DataBuilder withFrozenState()
        {
            NbtCompound tickingState = new NbtCompound();
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
        private DataBuilder withRule(CarpetRule<?> rule)
        {
            NbtCompound rules = (NbtCompound) tag.get("Rules");
            if (rules == null)
            {
                rules = new NbtCompound();
                tag.put("Rules", rules);
            }
            String identifier = rule.settingsManager().getIdentifier();
            String key = rule.name();
            while (rules.contains(key)) { key = key+"2";}
            NbtCompound ruleNBT = new NbtCompound();
            ruleNBT.putString("Value", RuleHelper.toRuleString(rule.value()));
            ruleNBT.putString("Manager", identifier);
            ruleNBT.putString("Rule", rule.name());
            rules.put(key, ruleNBT);
            return this;
        }

        public DataBuilder withCustomNbt(String key, NbtElement value)
        {
            tag.put(key, value);
            return this;
        }

        private PacketByteBuf build()
        {
            PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
            packetBuf.writeVarInt(CarpetClient.DATA);
            packetBuf.writeNbt(tag);
            return packetBuf;
        }


    }
}
