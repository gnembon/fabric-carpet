package carpet.logging;

import carpet.CarpetServer;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.logHelpers.PacketCounter;
import carpet.mixins.PlayerListHeaderS2CPacketMixin;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

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

    public static Map<ServerPlayerEntity, List<BaseText>> player_huds = new HashMap<>();

    //keyed with player names so unlogged players don't hold the reference
    public static final Map<String, BaseText> scarpet_headers = new HashMap<>();
    
    public static final Map<String, BaseText> scarpet_footers = new HashMap<>();

    public static void resetScarpetHUDs() {
        scarpet_headers.clear();
        scarpet_footers.clear();
    }

    public static void addMessage(ServerPlayerEntity player, BaseText hudMessage)
    {
        if (player == null) return;
        if (!player_huds.containsKey(player))
        {
            player_huds.put(player, new ArrayList<>());
        }
        else
        {
            player_huds.get(player).add(new LiteralText("\n"));
        }
        player_huds.get(player).add(hudMessage);
    }
    public static void clear_player(PlayerEntity player)
    {
        PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
        ((PlayerListHeaderS2CPacketMixin)packet).setHeader(new LiteralText(""));
        ((PlayerListHeaderS2CPacketMixin)packet).setFooter(new LiteralText(""));
        ((ServerPlayerEntity)player).networkHandler.sendPacket(packet);
    }


    public static void update_hud(MinecraftServer server, List<ServerPlayerEntity> force)
    {
        if (((server.getTicks() % 20 != 0) && force == null) || CarpetServer.minecraft_server == null)
            return;

        player_huds.clear();

        server.getPlayerManager().getPlayerList().forEach(p -> {
            BaseText scarpetFOoter = scarpet_footers.get(p.getEntityName());
            if (scarpetFOoter != null) HUDController.addMessage(p, scarpetFOoter);
        });

        if (LoggerRegistry.__tps)
            LoggerRegistry.getLogger("tps").log(()-> send_tps_display(server));

        if (LoggerRegistry.__mobcaps)
            LoggerRegistry.getLogger("mobcaps").log((option, player) -> {
                RegistryKey<World> dim = player.world.getRegistryKey(); //getDimType
                switch (option)
                {
                    case "overworld":
                        dim = World.OVERWORLD; // OW
                        break;
                    case "nether":
                        dim = World.NETHER; // nether
                        break;
                    case "end":
                        dim = World.END; // end
                        break;
                }
                return new BaseText[]{SpawnReporter.printMobcapsForDimension(server.getWorld(dim), false).get(0)};
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(()-> packetCounter());

        // extensions have time to pitch in.
        HUDListeners.forEach(l -> l.accept(server));

        Set<ServerPlayerEntity> targets = new HashSet<>(player_huds.keySet());
        if (force!= null) targets.addAll(force);
        for (ServerPlayerEntity player: targets)
        {
            PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
            ((PlayerListHeaderS2CPacketMixin)packet).setHeader(scarpet_headers.getOrDefault(player.getEntityName(), new LiteralText("")));
            ((PlayerListHeaderS2CPacketMixin)packet).setFooter(Messenger.c(player_huds.getOrDefault(player, Collections.emptyList()).toArray(new Object[0])));
            player.networkHandler.sendPacket(packet);
        }
    }
    private static BaseText [] send_tps_display(MinecraftServer server)
    {
        double MSPT = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
        double TPS = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0)?0.0:TickSpeed.mspt, MSPT);
        String color = Messenger.heatmap_color(MSPT,TickSpeed.mspt);
        return new BaseText[]{Messenger.c(
                "g TPS: ", String.format(Locale.US, "%s %.1f",color, TPS),
                "g  MSPT: ", String.format(Locale.US,"%s %.1f", color, MSPT))};
    }

    private static BaseText [] send_counter_info(MinecraftServer server, String color)
    {
        List <BaseText> res = new ArrayList<>();
        Arrays.asList(color.split(",")).forEach(c ->{
            HopperCounter counter = HopperCounter.getCounter(c);
            if (counter != null) res.addAll(counter.format(server, false, true));
        });
        return res.toArray(new BaseText[0]);
    }
    private static BaseText [] packetCounter()
    {
        BaseText [] ret =  new BaseText[]{
                Messenger.c("w I/" + PacketCounter.totalIn + " O/" + PacketCounter.totalOut),
        };
        PacketCounter.reset();
        return ret;
    }
}
