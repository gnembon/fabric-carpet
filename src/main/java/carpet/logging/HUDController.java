package carpet.logging;

import carpet.CarpetServer;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.logHelpers.PacketCounter;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;

public class HUDController
{
    private static List<Consumer<MinecraftServer>> HUDListeners = new ArrayList<>();

    /**
     * Adds listener to be called when HUD is updated for logging information
     * @param listener - a method to be called when new HUD inforation are collected
     */
    public static void register(Consumer<MinecraftServer> listener)
    {
        HUDListeners.add(listener);
    }

    public static Map<ServerPlayer, List<Component>> player_huds = new HashMap<>();
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
    public static void clear_player(Player player)
    {
        FriendlyByteBuf packetData = new FriendlyByteBuf(Unpooled.buffer()).writeComponent(Component.literal("")).writeComponent(Component.literal(""));
        ClientboundTabListPacket packet = new ClientboundTabListPacket(packetData);
        //((PlayerListHeaderS2CPacketMixin)packet).setHeader(new LiteralText(""));
        //((PlayerListHeaderS2CPacketMixin)packet).setFooter(new LiteralText(""));
        ((ServerPlayer)player).connection.send(packet);
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
                ResourceKey<Level> dim = player.level.dimension(); //getDimType
                switch (option)
                {
                    case "overworld":
                        dim = Level.OVERWORLD; // OW
                        break;
                    case "nether":
                        dim = Level.NETHER; // nether
                        break;
                    case "end":
                        dim = Level.END; // end
                        break;
                }
                return new Component[]{SpawnReporter.printMobcapsForDimension(server.getLevel(dim), false).get(0)};
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(()-> packetCounter());

        if (LoggerRegistry.__scheduledTicks)
            LoggerRegistry.getLogger("scheduledTicks").log((option,player)-> {
                if (Objects.equals(option, "global")) {
                    return scheduledTicksCountGlobal(server);
                } else {
                    ResourceKey<Level> dim = switch (option) {
                        case "overworld" -> Level.OVERWORLD; // OW
                        case "nether" -> Level.NETHER; // nether
                        case "end" -> Level.END; // end
                        default -> player.level.dimension(); //getDimType
                    };
                    return scheduledTicksCount(server.getLevel(dim));
                }
            });

        // extensions have time to pitch in.
        HUDListeners.forEach(l -> l.accept(server));

        Set<ServerPlayer> targets = new HashSet<>(player_huds.keySet());
        if (force!= null) targets.addAll(force);
        for (ServerPlayer player: targets)
        {
            FriendlyByteBuf packetData = new FriendlyByteBuf(Unpooled.buffer()).
                    writeComponent(scarpet_headers.getOrDefault(player.getScoreboardName(), Component.literal(""))).
                    writeComponent(Messenger.c(player_huds.getOrDefault(player, Collections.emptyList()).toArray(new Object[0])));
            ClientboundTabListPacket packet = new ClientboundTabListPacket(packetData);

            //PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
            //((PlayerListHeaderS2CPacketMixin)packet).setHeader(scarpet_headers.getOrDefault(player.getEntityName(), new LiteralText("")));
            //((PlayerListHeaderS2CPacketMixin)packet).setFooter(Messenger.c(player_huds.getOrDefault(player, Collections.emptyList()).toArray(new Object[0])));
            player.connection.send(packet);
        }
    }
    private static Component [] send_tps_display(MinecraftServer server)
    {
        double MSPT = Mth.average(server.tickTimes) * 1.0E-6D;
        double TPS = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0)?0.0:TickSpeed.mspt, MSPT);
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
    private static Component [] scheduledTicksCount(ServerLevel level)
    {
        return new Component[]{
                Messenger.c("w Ticks - Block: " + level.getBlockTicks().count() + "  Fluid: " + level.getFluidTicks().count()),
        };
    }
    private static Component [] scheduledTicksCountGlobal(MinecraftServer server)
    {
        int blockTickCount = 0;
        int fluidTickCount = 0;
        for (ServerLevel level : server.getAllLevels()) {
            blockTickCount += level.getBlockTicks().count();
            fluidTickCount += level.getFluidTicks().count();
        }
        return new Component[]{
                Messenger.c("w Ticks - Block: " + blockTickCount + "  Fluid: " + fluidTickCount),
        };
    }
}
