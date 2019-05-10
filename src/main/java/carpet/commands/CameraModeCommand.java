package carpet.commands;

import carpet.CarpetSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.world.GameMode;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CameraModeCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> camera = literal("c").
                requires((player) -> CarpetSettings.getBool("commandCameramode")).
                executes((c) -> cameraMode(c.getSource(), c.getSource().getPlayer())).
                then(argument("player", EntityArgumentType.player()).
                        executes( (c) -> cameraMode(c.getSource(), EntityArgumentType.getPlayer(c, "player"))));

        LiteralArgumentBuilder<ServerCommandSource> survival = literal("s").
                requires((player) -> CarpetSettings.getBool("commandCameramode")).
                executes((c) -> survivalMode(
                        c.getSource(),
                        c.getSource().getPlayer())).
                then(argument("player", EntityArgumentType.player()).
                        executes( (c) -> survivalMode(c.getSource(), EntityArgumentType.getPlayer(c, "player"))));

        dispatcher.register(camera);
        dispatcher.register(survival);
    }
    private static boolean iCanHasPermissions(ServerCommandSource source, PlayerEntity player)
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
    private static int cameraMode(ServerCommandSource source, PlayerEntity player)
    {
        if (!(iCanHasPermissions(source, player))) return 0;
        player.setGameMode(GameMode.SPECTATOR);
        player.addPotionEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 999999, 0, false, false));
        player.addPotionEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 999999, 0, false, false));
        return 1;
    }
    private static int survivalMode(ServerCommandSource source, PlayerEntity player)
    {
        if (!(iCanHasPermissions(source, player))) return 0;
        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(StatusEffects.NIGHT_VISION);
        player.removePotionEffect(StatusEffects.CONDUIT_POWER);
        return 1;
    }

}
