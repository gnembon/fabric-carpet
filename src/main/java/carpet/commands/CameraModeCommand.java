package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CameraModeCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> camera = literal("c").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandCameramode)).
                executes((c) -> cameraMode(c.getSource(), c.getSource().getPlayer())).
                then(argument("player", EntityArgumentType.player()).
                        executes( (c) -> cameraMode(c.getSource(), EntityArgumentType.getPlayer(c, "player"))));

        LiteralArgumentBuilder<ServerCommandSource> survival = literal("s").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandCameramode)).
                executes((c) -> survivalMode(
                        c.getSource(),
                        c.getSource().getPlayer())).
                then(argument("player", EntityArgumentType.player()).
                        executes( (c) -> survivalMode(c.getSource(), EntityArgumentType.getPlayer(c, "player"))));

        dispatcher.register(camera);
        dispatcher.register(survival);
    }
    private static boolean iCanHasPermissions(ServerCommandSource source, ServerPlayerEntity player)
    {
        try
        {
            return source.hasPermissionLevel(2) || source.getPlayer() == player;
        }
        catch (CommandSyntaxException e)
        {
            return true; // shoudn't happen because server has all permissions anyways
        }
    }
    private static int cameraMode(ServerCommandSource source, ServerPlayerEntity player)
    {
        if (!(iCanHasPermissions(source, player))) return 0;
        player.setGameMode(GameMode.SPECTATOR);
        player.addVelocity(0,0.1,0);
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 999999, 0, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 999999, 0, false, false));
        return 1;
    }
    private static int survivalMode(ServerCommandSource source, ServerPlayerEntity player)
    {
        if (!(iCanHasPermissions(source, player))) return 0;
        player.setGameMode(GameMode.SURVIVAL);
        player.networkHandler.sendPacket(new RemoveEntityStatusEffectS2CPacket(player.getEntityId(), StatusEffects.NIGHT_VISION));
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        player.networkHandler.sendPacket(new RemoveEntityStatusEffectS2CPacket(player.getEntityId(), StatusEffects.CONDUIT_POWER));
        player.removeStatusEffect(StatusEffects.CONDUIT_POWER);
        return 1;
    }

}
