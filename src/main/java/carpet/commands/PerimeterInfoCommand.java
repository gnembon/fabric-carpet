package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import carpet.utils.PerimeterDiagnostics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.ResourceArgument.getSummonableEntityType;
import static net.minecraft.commands.arguments.ResourceArgument.resource;

public class PerimeterInfoCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("perimeterinfo").
                requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandPerimeterInfo)).
                executes( (c) -> perimeterDiagnose(
                        c.getSource(),
                        new BlockPos(c.getSource().getPosition()),
                        null)).
                then(argument("center position", BlockPosArgument.blockPos()).
                        executes( (c) -> perimeterDiagnose(
                                c.getSource(),
                                BlockPosArgument.getSpawnablePos(c, "center position"),
                                null)).
                        then(argument("mob", resource(commandBuildContext, Registries.ENTITY_TYPE)).
                                suggests(SuggestionProviders.SUMMONABLE_ENTITIES).
                                executes( (c) -> perimeterDiagnose(
                                        c.getSource(),
                                        BlockPosArgument.getSpawnablePos(c, "center position"),
                                        getSummonableEntityType(c, "mob").key().location().toString()
                                ))));
        dispatcher.register(command);
    }

    private static int perimeterDiagnose(CommandSourceStack source, BlockPos pos, String mobId)
    {
        CompoundTag nbttagcompound = new CompoundTag();
        Mob entityliving = null;
        if (mobId != null)
        {
            nbttagcompound.putString("id", mobId);
            Entity baseEntity = EntityType.loadEntityRecursive(nbttagcompound, source.getLevel(), (entity_1x) -> {
                entity_1x.moveTo(new BlockPos(pos.getX(), source.getLevel().getMinBuildHeight()-10, pos.getZ()), entity_1x.getYRot(), entity_1x. getXRot());
                return !source.getLevel().addWithUUID(entity_1x) ? null : entity_1x;
            });
            if (!(baseEntity instanceof  Mob))
            {
                Messenger.m(source, "r /perimeterinfo requires a mob entity to test against.");
                if (baseEntity != null) baseEntity.discard();
                return 0;
            }
            entityliving = (Mob) baseEntity;
        }
        PerimeterDiagnostics.Result res = PerimeterDiagnostics.countSpots(source.getLevel(), pos, entityliving);

        Messenger.m(source, "w Spawning spaces around ",Messenger.tp("c",pos), "w :");
        Messenger.m(source, "w   potential in-liquid: ","wb "+res.liquid);
        Messenger.m(source, "w   potential on-ground: ","wb "+res.ground);
        if (entityliving != null)
        {
            Messenger.m(source, "w   ", entityliving.getDisplayName() ,"w : ","wb "+res.specific);
            res.samples.forEach(bp -> Messenger.m(source, "w   ", Messenger.tp("c", bp)));
            entityliving.discard(); // dicard // remove();
        }
        return 1;
    }
}
