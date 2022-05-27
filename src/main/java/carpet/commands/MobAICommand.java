package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.MobAI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.core.Registry;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class MobAICommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("track").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandTrackAI)).
                then(argument("entity type", EntitySummonArgument.id()).

                        suggests( (c, b) -> suggest(MobAI.availbleTypes(), b)).
                        then(literal("clear").executes( (c) ->
                                {
                                    MobAI.clearTracking(Registry.ENTITY_TYPE.get(EntitySummonArgument.getSummonableEntity(c, "entity type")));
                                    return 1;
                                }
                        )).
                        then(argument("aspect", StringArgumentType.word()).
                                suggests( (c, b) -> suggest(MobAI.availableFor(Registry.ENTITY_TYPE.get(EntitySummonArgument.getSummonableEntity(c, "entity type"))),b)).
                                executes( (c) -> {
                                    MobAI.startTracking(
                                            Registry.ENTITY_TYPE.get(EntitySummonArgument.getSummonableEntity(c, "entity type")),
                                            MobAI.TrackingType.valueOf(StringArgumentType.getString(c, "aspect").toUpperCase())
                                    );
                                    return 1;
                                })));
        dispatcher.register(command);
    }
}
