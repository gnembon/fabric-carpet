package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.CommandHelper;
import carpet.utils.MobAI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;
import static net.minecraft.commands.arguments.ResourceArgument.getSummonableEntityType;
import static net.minecraft.commands.arguments.ResourceArgument.resource;

public class MobAICommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("track").
                requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandTrackAI)).
                then(argument("entity type", resource(commandBuildContext, Registries.ENTITY_TYPE)).

                        suggests( (c, b) -> suggest(MobAI.availbleTypes(), b)).
                        then(literal("clear").executes( (c) ->
                                {
                                    MobAI.clearTracking(getSummonableEntityType(c, "entity type").value());
                                    return 1;
                                }
                        )).
                        then(argument("aspect", StringArgumentType.word()).
                                suggests( (c, b) -> suggest(MobAI.availableFor(getSummonableEntityType(c, "entity type").value()),b)).
                                executes( (c) -> {
                                    MobAI.startTracking(
                                            getSummonableEntityType(c, "entity type").value(),
                                            MobAI.TrackingType.valueOf(StringArgumentType.getString(c, "aspect").toUpperCase())
                                    );
                                    return 1;
                                })));
        dispatcher.register(command);
    }
}
