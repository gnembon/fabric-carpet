package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.Messenger;
import carpet.utils.PerimeterDiagnostics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PerimeterInfoCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("perimeterinfo").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandPerimeterInfo)).
                executes( (c) -> perimeterDiagnose(
                        c.getSource(),
                        new BlockPos(c.getSource().getPosition()),
                        null)).
                then(argument("center position", BlockPosArgumentType.blockPos()).
                        executes( (c) -> perimeterDiagnose(
                                c.getSource(),
                                BlockPosArgumentType.getBlockPos(c, "center position"),
                                null)).
                        then(argument("mob",EntitySummonArgumentType.entitySummon()).
                                suggests(SuggestionProviders.SUMMONABLE_ENTITIES).
                                executes( (c) -> perimeterDiagnose(
                                        c.getSource(),
                                        BlockPosArgumentType.getBlockPos(c, "center position"),
                                        EntitySummonArgumentType.getEntitySummon(c, "mob").toString()
                                ))));
        dispatcher.register(command);
    }

    private static int perimeterDiagnose(ServerCommandSource source, BlockPos pos, String mobId)
    {
        NbtCompound nbttagcompound = new NbtCompound();
        MobEntity entityliving = null;
        if (mobId != null)
        {
            nbttagcompound.putString("id", mobId);
            Entity baseEntity = EntityType.loadEntityWithPassengers(nbttagcompound, source.getWorld(), (entity_1x) -> {
                entity_1x.refreshPositionAndAngles(new BlockPos(pos.getX(), source.getWorld().getBottomY()-10, pos.getZ()), entity_1x.getYaw(), entity_1x. getPitch());
                return !source.getWorld().tryLoadEntity(entity_1x) ? null : entity_1x;
            });
            if (!(baseEntity instanceof  MobEntity))
            {
                Messenger.m(source, "r /perimeterinfo requires a mob entity to test agains.");
                if (baseEntity != null) baseEntity.discard();
                return 0;
            }
            entityliving = (MobEntity) baseEntity;
        }
        PerimeterDiagnostics.Result res = PerimeterDiagnostics.countSpots(source.getWorld(), pos, entityliving);

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
