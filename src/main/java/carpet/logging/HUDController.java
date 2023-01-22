package carpet.logging;

import carpet.CarpetServer;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.logHelpers.PacketCounter;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Consumer;

public class HUDController
{
    private static final List<Consumer<MinecraftServer>> HUDListeners = new ArrayList<>();

    /**
     * Adds listener to be called when HUD is updated for logging information
     * @param listener - a method to be called when new HUD inforation are collected
     */
    public static void register(Consumer<MinecraftServer> listener)
    {
        HUDListeners.add(listener);
    }

    public static final Map<ServerPlayer, List<Component>> player_huds = new HashMap<>();
//keyed with player names so unlogged players don't hold the reference
    public static final Map<String, Component> scarpet_headers = new HashMap<>();

    public static final Map<String, Component> scarpet_footers = new HashMap<>();

    public static void resetScarpetHUDs() {
        scarpet_headers.clear();
        scarpet_footers.clear();
    }

    public static void addMessage(ServerPlayer player, Component hudMessage)
    {
        if (player == null) return;
        if (!player_huds.containsKey(player))
        {
            player_huds.put(player, new ArrayList<>());
        }
        else
        {
            player_huds.get(player).add(Component.literal("\n"));
        }
        player_huds.get(player).add(hudMessage);
    }

    public static void clearPlayer(ServerPlayer player)
    {
        ClientboundTabListPacket packet = new ClientboundTabListPacket(Component.literal(""), Component.literal(""));
        player.connection.send(packet);
    }


    public static void update_hud(MinecraftServer server, List<ServerPlayer> force)
    {
        if (((server.getTickCount() % 20 != 0) && force == null) || CarpetServer.minecraft_server == null)
            return;

        player_huds.clear();

        server.getPlayerList().getPlayers().forEach(p -> {
            Component scarpetFOoter = scarpet_footers.get(p.getScoreboardName());
            if (scarpetFOoter != null) HUDController.addMessage(p, scarpetFOoter);
        });

        if (LoggerRegistry.__tps)
            LoggerRegistry.getLogger("tps").log(()-> send_tps_display(server));

        if (LoggerRegistry.__mobcaps)
            LoggerRegistry.getLogger("mobcaps").log((option, player) -> {
                ResourceKey<Level> dim = switch (option) {
                    case "overworld" -> Level.OVERWORLD;
                    case "nether" -> Level.NETHER;
                    case "end" -> Level.END;
                    default -> player.level.dimension();
                };
                return new Component[]{SpawnReporter.printMobcapsForDimension(server.getLevel(dim), false).get(0)};
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(HUDController::packetCounter);

        // extensions have time to pitch in.
        HUDListeners.forEach(l -> l.accept(server));

        Set<ServerPlayer> targets = new HashSet<>(player_huds.keySet());
        if (force!= null) targets.addAll(force);
        for (ServerPlayer player: targets)
        {
            ClientboundTabListPacket packet = new ClientboundTabListPacket(
                        scarpet_headers.getOrDefault(player.getScoreboardName(), Component.literal("")),
                        Messenger.c(player_huds.getOrDefault(player, List.of()).toArray(new Object[0]))
                    );
            player.connection.send(packet);
        }
    }
    private static Component [] send_tps_display(MinecraftServer server)
    {
        final OptionalDouble averageTPS = Arrays.stream(server.tickTimes).average();
        if (averageTPS.isEmpty())
        {
            return new Component[]{Component.literal("No TPS data available")};
        }
        double MSPT = Arrays.stream(server.tickTimes).average().getAsDouble() * 1.0E-6D;
        double TPS = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0)?0.0:TickSpeed.mspt, MSPT);
        if (TickSpeed.isPaused()) {
            TPS = 0;
        }
        String color = Messenger.heatmap_color(MSPT,TickSpeed.mspt);
        return new Component[]{Messenger.c(
                "g TPS: ", String.format(Locale.US, "%s %.1f",color, TPS),
                "g  MSPT: ", String.format(Locale.US,"%s %.1f", color, MSPT))};
    }

    private static Component [] send_counter_info(MinecraftServer server, String color)
    {
        List <Component> res = new ArrayList<>();
        Arrays.asList(color.split(",")).forEach(c ->{
            HopperCounter counter = HopperCounter.getCounter(c);
            if (counter != null) res.addAll(counter.format(server, false, true));
        });
        return res.toArray(new Component[0]);
    }
    private static Component [] packetCounter()
    {
        Component [] ret =  new Component[]{
                Messenger.c("w I/" + PacketCounter.totalIn + " O/" + PacketCounter.totalOut),
        };
        PacketCounter.reset();
        return ret;
    }
}
