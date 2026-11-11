package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.CommandHelper;
import carpet.utils.MobAI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringArgument;
import net.minecraft.core.registries.Registries;

import static net.minecraft.commands.suggestions.SuggestionProviders.serverOnly;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.suggestions.SuggestionContext.suggest;
import static net.minecraft.commands.arguments.ResourceArgument.getSummonableEntityType;
import static net.minecraft.commands.arguments.ResourceArgument.resource;

public class MobAICommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("track").
                requires(CommandHelper.commandPermission(() -> CarpetSettings.commandTrackAI)).
                then(argument("entity type", resource(Registries.ENTITY_TYPE)).

                        suggests(serverOnly( (server, c, b) -> suggest(MobAI.availbleTypes(server.createCommandSourceStack()), b))).
                        then(literal("clear").executes( (c) ->
                                {
                                    MobAI.clearTracking(c.getSource().getServer(), getSummonableEntityType(c, "entity type").value());
                                    return 1;
                                }
                        )).
                        then(argument("aspect", StringArgument.word()).
                                suggests(serverOnly( (server, c, b) -> suggest(MobAI.availableFor(getSummonableEntityType(c, "entity type").value()),b))).
                                executes( (c) -> {
                                    MobAI.startTracking(
                                            getSummonableEntityType(c, "entity type").value(),
                                            MobAI.TrackingType.valueOf(StringArgument.getString(c, "aspect").toUpperCase())
                                    );
                                    return 1;
                                })));
        dispatcher.register(command);
    }
}
