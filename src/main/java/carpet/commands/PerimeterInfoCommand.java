package carpet.commands;

import carpet.settings.CarpetSettings;
import carpet.utils.Messenger;
import carpet.utils.PerimeterDiagnostics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.EntitySummonArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PerimeterInfoCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("perimeterinfo").
                requires((player) -> CarpetSettings.commandPerimeterInfo).
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
        CompoundTag nbttagcompound = new CompoundTag();
        MobEntity entityliving = null;
        if (mobId != null)
        {
            nbttagcompound.putString("id", mobId);
            entityliving = (MobEntity) EntityType.loadEntityWithPassengers(nbttagcompound, source.getWorld(), (entity_1x) -> {
                entity_1x.setPositionAndAngles(pos.getX(), pos.getY()+2, pos.getZ(), entity_1x.yaw, entity_1x.pitch);
                return !source.getWorld().method_18768(entity_1x) ? null : entity_1x;
            });
            if (entityliving == null)
            {
                Messenger.m(source, "r Failed to spawn test entity");
                return 0;
            }
        }
        PerimeterDiagnostics.Result res = PerimeterDiagnostics.countSpots(source.getWorld(), pos, entityliving);

        Messenger.m(source, "w Spawning spaces around ",Messenger.tp("c",pos), "w :");
        Messenger.m(source, "w   potential in-liquid: ","wb "+res.liquid);
        Messenger.m(source, "w   potential on-ground: ","wb "+res.ground);
        if (entityliving != null)
        {
            Messenger.m(source, "w   ", entityliving.getDisplayName() ,"w : ","wb "+res.specific);
            res.samples.forEach(bp -> Messenger.m(source, "w   ", Messenger.tp("c", bp)));
            entityliving.remove();
        }
        return 1;
    }
}
