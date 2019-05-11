package carpet.utils;

import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.PacketCounter;
import carpet.mixins.logging.PlayerListHeaderS2CPacketMixin;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.client.network.packet.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HUDController
{
    public static Map<PlayerEntity, List<BaseComponent>> player_huds = new HashMap<>();

    public static void addMessage(PlayerEntity player, BaseComponent hudMessage)
    {
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
    public static void clear_player(PlayerEntity player)
    {
        PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
        ((PlayerListHeaderS2CPacketMixin)packet).setHeader(new TextComponent(""));
        ((PlayerListHeaderS2CPacketMixin)packet).setFooter(new TextComponent(""));
        ((ServerPlayerEntity)player).networkHandler.sendPacket(packet);
    }


    public static void update_hud(MinecraftServer server)
    {
        if(server.getTicks() % 20 != 0)
            return;

        player_huds.clear();

        if (LoggerRegistry.__tps)
            LoggerRegistry.getLogger("tps").log(()-> send_tps_display(server));

        if (LoggerRegistry.__mobcaps)
            LoggerRegistry.getLogger("mobcaps").log((option, player) -> {
                int dim = player.dimension.getRawId();
                switch (option)
                {
                    case "overworld":
                        dim = 0;
                        break;
                    case "nether":
                        dim = -1;
                        break;
                    case "end":
                        dim = 1;
                        break;
                }
                return send_mobcap_display(dim);
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(()-> packetCounter());

        for (PlayerEntity player: player_huds.keySet())
        {
            PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
            ((PlayerListHeaderS2CPacketMixin)packet).setHeader(new TextComponent(""));
            ((PlayerListHeaderS2CPacketMixin)packet).setFooter(Messenger.c(player_huds.get(player).toArray(new Object[0])));
            ((ServerPlayerEntity)player).networkHandler.sendPacket(packet);
        }
    }
    private static BaseComponent [] send_tps_display(MinecraftServer server)
    {
        double MSPT = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
        double TPS = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0)?0.0:TickSpeed.mspt, MSPT);
        String color = Messenger.heatmap_color(MSPT,TickSpeed.mspt);
        return new BaseComponent[]{Messenger.c(
                "g TPS: ", String.format(Locale.US, "%s %.1f",color, TPS),
                "g  MSPT: ", String.format(Locale.US,"%s %.1f", color, MSPT))};
    }

    private static BaseComponent [] send_mobcap_display(int dim)
    {
        List<BaseComponent> components = new ArrayList<>();
        for (EntityCategory type:EntityCategory.values())
        {
            Pair<Integer,Integer> counts = SpawnReporter.mobcaps.get(dim).getOrDefault(type, new Pair<>(0, 0));
            int actual = counts.getLeft(); int limit = counts.getRight();
            components.add(Messenger.c(
                    (actual+limit == 0)?"g -":Messenger.heatmap_color(actual,limit)+" "+actual,
                    Messenger.creatureTypeColor(type)+" /"+((actual+limit == 0)?"-":limit)
                    ));
            components.add(Messenger.c("w  "));
        }
        components.remove(components.size()-1);
        return new BaseComponent[]{Messenger.c(components.toArray(new Object[0]))};
    }

    private static BaseComponent [] send_counter_info(MinecraftServer server, String color)
    {
        HopperCounter counter = HopperCounter.getCounter(color);
        List <BaseComponent> res = counter == null ? Collections.emptyList() : counter.format(server, false, true);
        return new BaseComponent[]{ Messenger.c(res.toArray(new Object[0]))};
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
