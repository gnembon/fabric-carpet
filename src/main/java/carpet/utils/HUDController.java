package carpet.utils;

import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.PacketCounter;
import carpet.mixins.PlayerListHeaderS2CPacketMixin;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.client.network.packet.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HUDController
{
    public static Map<PlayerEntity, List<BaseText>> player_huds = new HashMap<>();

    public static void addMessage(PlayerEntity player, BaseText hudMessage)
    {
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


    public static void update_hud(MinecraftServer server)
    {
        if(server.getTicks() % 20 != 0)
            return;

        player_huds.clear();

        if (LoggerRegistry.__tps)
            LoggerRegistry.getLogger("tps").log(()-> send_tps_display(server));

        if (LoggerRegistry.__mobcaps)
            LoggerRegistry.getLogger("mobcaps").log((option, player) -> {
                DimensionType dim = player.dimension;
                switch (option)
                {
                    case "overworld":
                        dim = DimensionType.OVERWORLD;
                        break;
                    case "nether":
                        dim = DimensionType.THE_NETHER;
                        break;
                    case "end":
                        dim = DimensionType.THE_END;
                        break;
                }
                return new BaseText[]{SpawnReporter.printMobcapsForDimension(dim, false).get(0)};
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(()-> packetCounter());

        for (PlayerEntity player: player_huds.keySet())
        {
            PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket();
            ((PlayerListHeaderS2CPacketMixin)packet).setHeader(new LiteralText(""));
            ((PlayerListHeaderS2CPacketMixin)packet).setFooter(Messenger.c(player_huds.get(player).toArray(new Object[0])));
            ((ServerPlayerEntity)player).networkHandler.sendPacket(packet);
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
        HopperCounter counter = HopperCounter.getCounter(color);
        List <BaseText> res = counter == null ? Collections.emptyList() : counter.format(server, false, true);
        return new BaseText[]{ Messenger.c(res.toArray(new Object[0]))};
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
