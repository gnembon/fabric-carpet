package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.fakes.ServerGamePacketListenerImplInterface;
import carpet.script.utils.SnoopyCommandSource;

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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerNetworkHandler
{
    private static final Map<ServerPlayer, String> remoteCarpetPlayers = new HashMap<>();
    private static final Set<ServerPlayer> validCarpetPlayers = new HashSet<>();

    private static final Map<String, BiConsumer<ServerPlayer, Tag>> dataHandlers = Map.of(
            CarpetClient.HELLO, (p, t) -> onHello(p, t.getAsString()),
            "clientCommand", (p, t) -> handleClientCommand(p, (CompoundTag) t)
    );

    public static void onPlayerJoin(ServerPlayer playerEntity)
    {
        if (!((ServerGamePacketListenerImplInterface) playerEntity.connection).getConnection().isMemoryConnection())
        {
            CompoundTag data = new CompoundTag();
            data.putString(CarpetClient.HI, CarpetSettings.carpetVersion);
            playerEntity.connection.send(new ClientboundCustomPayloadPacket(new CarpetClient.CarpetPayload(data)));
        }
        else
        {
            validCarpetPlayers.add(playerEntity);
        }
    }

    public static void onHello(ServerPlayer playerEntity, String version)
    {
        validCarpetPlayers.add(playerEntity);
        remoteCarpetPlayers.put(playerEntity, version);
        if (version.equals(CarpetSettings.carpetVersion))
        {
            CarpetSettings.LOG.info("Player " + playerEntity.getName().getString() + " joined with a matching carpet client");
        }
        else
        {
            CarpetSettings.LOG.warn("Player " + playerEntity.getName().getString() + " joined with another carpet version: " + version);
        }
        DataBuilder data = DataBuilder.create(playerEntity.server); // tickrate related settings are sent on world change
        CarpetServer.forEachManager(sm -> sm.getCarpetRules().forEach(data::withRule));
        playerEntity.connection.send(data.build());
    }

    public static void sendPlayerLevelData(ServerPlayer player, ServerLevel level)
    {
        if (CarpetSettings.superSecretSetting || !validCarpetPlayers.contains(player))
        {
            //return;
        }
        // noop, used to send ticking information
        //DataBuilder data = DataBuilder.create(player.server);//.withTickRate().withFrozenState().withTickPlayerActiveTimeout(); // .withSuperHotState()
        //player.connection.send(data.build());
    }

    private static void handleClientCommand(ServerPlayer player, CompoundTag commandData)
    {
        String command = commandData.getString("command");
        String id = commandData.getString("id");
        List<Component> output = new ArrayList<>();
        Component[] error = {null};
        if (player.getServer() == null)
        {
            error[0] = Component.literal("No Server");
        }
        else
        {
            player.getServer().getCommands().performPrefixedCommand(
                    new SnoopyCommandSource(player, error, output), command
            );
        }
        CompoundTag result = new CompoundTag();
        result.putString("id", id);
        if (error[0] != null)
        {
            result.putString("error", error[0].getContents().toString());
        }
        ListTag outputResult = new ListTag();
        for (Component line : output)
        {
            outputResult.add(StringTag.valueOf(Component.Serializer.toJson(line, player.registryAccess())));
        }
        if (!output.isEmpty())
        {
            result.put("output", outputResult);
        }
        player.connection.send(DataBuilder.create(player.server).withCustomNbt("clientCommand", result).build());
        // run command plug to command output,
    }

    public static void onClientData(ServerPlayer player, CompoundTag compound)
    {
        for (String key : compound.getAllKeys())
        {
            if (dataHandlers.containsKey(key))
            {
                dataHandlers.get(key).accept(player, compound.get(key));
            }
            else
            {
                CarpetSettings.LOG.warn("Unknown carpet client data: " + key);
            }
        }
    }

    public static void updateRuleWithConnectedClients(CarpetRule<?> rule)
    {
        if (CarpetSettings.superSecretSetting)
        {
            return;
        }
        for (ServerPlayer player : remoteCarpetPlayers.keySet())
        {
            player.connection.send(DataBuilder.create(player.server).withRule(rule).build());
        }
    }

    public static void broadcastCustomCommand(String command, Tag data)
    {
        if (CarpetSettings.superSecretSetting)
        {
            return;
        }
        for (ServerPlayer player : validCarpetPlayers)
        {
            player.connection.send(DataBuilder.create(player.server).withCustomNbt(command, data).build());
        }
    }

    public static void sendCustomCommand(ServerPlayer player, String command, Tag data)
    {
        if (isValidCarpetPlayer(player))
        {
            player.connection.send(DataBuilder.create(player.server).withCustomNbt(command, data).build());
        }
    }

    public static void onPlayerLoggedOut(ServerPlayer player)
    {
        validCarpetPlayers.remove(player);
        if (!((ServerGamePacketListenerImplInterface) player.connection).getConnection().isMemoryConnection())
        {
            remoteCarpetPlayers.remove(player);
        }
    }

    public static void close()
    {
        remoteCarpetPlayers.clear();
        validCarpetPlayers.clear();
    }

    public static boolean isValidCarpetPlayer(ServerPlayer player)
    {
        if (CarpetSettings.superSecretSetting)
        {
            return false;
        }
        return validCarpetPlayers.contains(player);

    }

    public static String getPlayerStatus(ServerPlayer player)
    {
        if (remoteCarpetPlayers.containsKey(player))
        {
            return "carpet " + remoteCarpetPlayers.get(player);
        }
        if (validCarpetPlayers.contains(player))
        {
            return "carpet " + CarpetSettings.carpetVersion;
        }
        return "vanilla";
    }

    private static class DataBuilder
    {
        private CompoundTag tag;
        // unused now, but hey
        private MinecraftServer server;

        private static DataBuilder create(final MinecraftServer server)
        {
            return new DataBuilder(server);
        }

        private DataBuilder(MinecraftServer server)
        {
            tag = new CompoundTag();
            this.server = server;
        }

        private DataBuilder withRule(CarpetRule<?> rule)
        {
            CompoundTag rules = (CompoundTag) tag.get("Rules");
            if (rules == null)
            {
                rules = new CompoundTag();
                tag.put("Rules", rules);
            }
            String identifier = rule.settingsManager().identifier();
            String key = rule.name();
            while (rules.contains(key))
            {
                key = key + "2";
            }
            CompoundTag ruleNBT = new CompoundTag();
            ruleNBT.putString("Value", RuleHelper.toRuleString(rule.value()));
            ruleNBT.putString("Manager", identifier);
            ruleNBT.putString("Rule", rule.name());
            rules.put(key, ruleNBT);
            return this;
        }

        public DataBuilder withCustomNbt(String key, Tag value)
        {
            tag.put(key, value);
            return this;
        }

        private ClientboundCustomPayloadPacket build()
        {
            return new ClientboundCustomPayloadPacket(new CarpetClient.CarpetPayload(tag));
        }
    }
}
