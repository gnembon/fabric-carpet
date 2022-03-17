package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import static carpet.commands.TickCommand.healthEntities;
import static carpet.commands.TickCommand.healthReport;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ProfileCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal("profile").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandProfile)).
                executes( (c) -> healthReport(c.getSource(), 100)).
                then(literal("health").
                        executes( (c) -> healthReport(c.getSource(), 100)).
                        then(argument("ticks", integer(20,24000)).
                                executes( (c) -> healthReport(c.getSource(), getInteger(c, "ticks"))))).
                then(literal("entities").
                        executes((c) -> healthEntities(c.getSource(), 100)).
                        then(argument("ticks", integer(20,24000)).
                                executes((c) -> healthEntities(c.getSource(), getInteger(c, "ticks")))));
        dispatcher.register(literalargumentbuilder);
    }
}
