package carpet.logging;

import carpet.CarpetServer;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.logHelpers.PacketCounter;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    public static Map<ServerPlayer, List<BaseComponent>> player_huds = new HashMap<>();
//keyed with player names so unlogged players don't hold the reference
    public static final Map<String, BaseComponent> scarpet_headers = new HashMap<>();

    public static final Map<String, BaseComponent> scarpet_footers = new HashMap<>();

    public static void resetScarpetHUDs() {
        scarpet_headers.clear();
        scarpet_footers.clear();
    }

    public static void addMessage(ServerPlayer player, BaseComponent hudMessage)
    {
        if (player == null) return;
        if (!player_huds.containsKey(player))
        {
            player_huds.put(player, new ArrayList<>());
        }
        else
        {
            player_huds.get(player).add(new TextComponent("\n"));
        }
        player_huds.get(player).add(hudMessage);
    }
    public static void clear_player(Player player)
    {
        FriendlyByteBuf packetData = new FriendlyByteBuf(Unpooled.buffer()).writeComponent(new TextComponent("")).writeComponent(new TextComponent(""));
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
            BaseComponent scarpetFOoter = scarpet_footers.get(p.getScoreboardName());
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
                return new BaseComponent[]{SpawnReporter.printMobcapsForDimension(server.getLevel(dim), false).get(0)};
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(()-> packetCounter());

        // extensions have time to pitch in.
        HUDListeners.forEach(l -> l.accept(server));

        Set<ServerPlayer> targets = new HashSet<>(player_huds.keySet());
        if (force!= null) targets.addAll(force);
        for (ServerPlayer player: targets)
        {
            FriendlyByteBuf packetData = new FriendlyByteBuf(Unpooled.buffer()).
                    writeComponent(scarpet_headers.getOrDefault(player.getScoreboardName(), new TextComponent(""))).
                    writeComponent(Messenger.c(player_huds.getOrDefault(player, Collections.emptyList()).toArray(new Object[0])));
            ClientboundTabListPacket packet = new ClientboundTabListPacket(packetData);

            //PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
            //((PlayerListHeaderS2CPacketMixin)packet).setHeader(scarpet_headers.getOrDefault(player.getEntityName(), new LiteralText("")));
            //((PlayerListHeaderS2CPacketMixin)packet).setFooter(Messenger.c(player_huds.getOrDefault(player, Collections.emptyList()).toArray(new Object[0])));
            player.connection.send(packet);
        }
    }
    private static BaseComponent [] send_tps_display(MinecraftServer server)
    {
        double MSPT = Mth.average(server.tickTimes) * 1.0E-6D;
        double TPS = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0)?0.0:TickSpeed.mspt, MSPT);
        String color = Messenger.heatmap_color(MSPT,TickSpeed.mspt);
        return new BaseComponent[]{Messenger.c(
                "g TPS: ", String.format(Locale.US, "%s %.1f",color, TPS),
                "g  MSPT: ", String.format(Locale.US,"%s %.1f", color, MSPT))};
    }

    private static BaseComponent [] send_counter_info(MinecraftServer server, String color)
    {
        List <BaseComponent> res = new ArrayList<>();
        Arrays.asList(color.split(",")).forEach(c ->{
            HopperCounter counter = HopperCounter.getCounter(c);
            if (counter != null) res.addAll(counter.format(server, false, true));
        });
        return res.toArray(new BaseComponent[0]);
    }
    private static BaseComponent [] packetCounter()
    {
        BaseComponent [] ret =  new BaseComponent[]{
                Messenger.c("w I/" + PacketCounter.totalIn + " O/" + PacketCounter.totalOut),
        };
        PacketCounter.reset();
        return ret;
    }
}
