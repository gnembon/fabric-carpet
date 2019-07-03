package carpet.commands;

import carpet.settings.CarpetSettings;
import carpet.utils.DistanceCalculator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.Vec3ArgumentType;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DistanceCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("distance").
                requires((player) -> CarpetSettings.commandDistance).
                then(literal("from").
                        executes( (c) -> DistanceCalculator.setStart(c.getSource(), c.getSource().getPosition())).
                        then(argument("from", Vec3ArgumentType.vec3()).
                                executes( (c) -> DistanceCalculator.setStart(
                                        c.getSource(),
                                        Vec3ArgumentType.getVec3(c, "from"))).
                                then(literal("to").
                                        executes((c) -> DistanceCalculator.distance(
                                                c.getSource(),
                                                Vec3ArgumentType.getVec3(c, "from"),
                                                c.getSource().getPosition())).
                                        then(argument("to", Vec3ArgumentType.vec3()).
                                                executes( (c) -> DistanceCalculator.distance(
                                                        c.getSource(),
                                                        Vec3ArgumentType.getVec3(c, "from"),
                                                        Vec3ArgumentType.getVec3(c, "to")
                                                )))))).
                then(literal("to").
                        executes( (c) -> DistanceCalculator.setEnd(c.getSource(), c.getSource().getPosition()) ).
                        then(argument("to", Vec3ArgumentType.vec3()).
                                executes( (c) -> DistanceCalculator.setEnd(c.getSource(), Vec3ArgumentType.getVec3(c, "to")))));
        dispatcher.register(command);
    }
}
