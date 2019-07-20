package carpet.commands;

import carpet.settings.CarpetSettings;
import carpet.utils.MobAI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.arguments.EntitySummonArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.Registry;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandSource.suggestMatching;

public class MobAICommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("track").
                requires((player) -> CarpetSettings.commandTrackAI).
                then(argument("entity type", EntitySummonArgumentType.entitySummon()).

                        suggests( (c, b) -> suggestMatching(MobAI.availbleTypes(), b)).
                        then(literal("clear").executes( (c) ->
                                {
                                    MobAI.clearTracking(Registry.ENTITY_TYPE.get(EntitySummonArgumentType.getEntitySummon(c, "entity type")));
                                    return 1;
                                }
                        )).
                        then(argument("aspect", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatching(MobAI.availableFor(Registry.ENTITY_TYPE.get(EntitySummonArgumentType.getEntitySummon(c, "entity type"))),b)).
                                executes( (c) -> {
                                    MobAI.startTracking(
                                            Registry.ENTITY_TYPE.get(EntitySummonArgumentType.getEntitySummon(c, "entity type")),
                                            MobAI.TrackingType.byName(StringArgumentType.getString(c, "aspect"))
                                    );
                                    return 1;
                                })));
        dispatcher.register(command);
    }
}
