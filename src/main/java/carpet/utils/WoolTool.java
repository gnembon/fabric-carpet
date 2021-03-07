package carpet.utils;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.BaseText;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WoolTool
{
    private static final HashMap<MaterialColor,DyeColor> Material2Dye = new HashMap<>();

    public static final HashMap<MaterialColor,String> Material2DyeName = new HashMap<MaterialColor, String>(){{
        put(MaterialColor.WHITE, "w ");
        put(MaterialColor.ORANGE, "#F9801D ");
        put(MaterialColor.MAGENTA, "m ");
        put(MaterialColor.LIGHT_BLUE, "t ");
        put(MaterialColor.YELLOW, "y ");
        put(MaterialColor.LAPIS, "l ");
        put(MaterialColor.PINK, "#FFACCB ");
        put(MaterialColor.GRAY, "f ");
        put(MaterialColor.LIGHT_GRAY, "g ");
        put(MaterialColor.CYAN, "c ");
        put(MaterialColor.PURPLE, "p ");
        put(MaterialColor.BLUE, "v ");
        put(MaterialColor.BROWN, "#835432 ");
        put(MaterialColor.GREEN, "e ");
        put(MaterialColor.RED, "r ");
        put(MaterialColor.BLACK, "k ");
    }};

    static
    {
        for (DyeColor color: DyeColor.values())
        {
            Material2Dye.put(color.getMaterialColor(),color);
        }
    }

    public static void carpetPlacedAction(DyeColor color, PlayerEntity placer, BlockPos pos, ServerWorld worldIn)
    {
		if (!CarpetSettings.carpets)
		{
			return;
		}
        switch (color)
        {
            case PINK:
                if (!"false".equals(CarpetSettings.commandSpawn))
                    Messenger.send(placer, SpawnReporter.report(pos, worldIn));

                break;
            case BLACK:
                if (!"false".equals(CarpetSettings.commandSpawn))
                    Messenger.send(placer, SpawnReporter.show_mobcaps(pos, worldIn));
                break;
            case BROWN:
                if (!"false".equals(CarpetSettings.commandDistance))
                {
                    ServerCommandSource source = placer.getCommandSource();
                    if (!DistanceCalculator.hasStartingPoint(source) || placer.isSneaking()) {
                        DistanceCalculator.setStart(source, Vec3d.of(pos) ); // zero padded pos
                    }
                    else {
                        DistanceCalculator.setEnd(source, Vec3d.of(pos));
                    }
                }
                break;
            case GRAY:
                if (!"false".equals(CarpetSettings.commandInfo))
                    Messenger.send(placer, BlockInfo.blockInfo(pos.down(), worldIn));
                break;
            case YELLOW:
                if (!"false".equals(CarpetSettings.commandInfo))
                    Messenger.m(placer, "r This used to show entity info around the player. Use data get entity command, sorry");
                    //EntityInfo.issue_entity_info(placer);
                break;
			case GREEN:
                if (CarpetSettings.hopperCounters)
                {
                    DyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter counter = HopperCounter.getCounter(under.toString());
                    if (counter != null)
                        Messenger.send(placer, counter.format(worldIn.getServer(), false, false));
                }
				break;
			case RED:
                if (CarpetSettings.hopperCounters)
                {
                    DyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter counter = HopperCounter.getCounter(under.toString());
                    if (counter == null) return;
                    counter.reset(placer.getServer());
                    List<BaseText> res = new ArrayList<>();
                    res.add(Messenger.s(String.format("%s counter reset",under.toString())));
                    Messenger.send(placer, res);
                }
			    break;
        }
    }

    public static DyeColor getWoolColorAtPosition(World worldIn, BlockPos pos)
    {
        BlockState state = worldIn.getBlockState(pos);
        if (state.getMaterial() != Material.WOOL || !state.isSolidBlock(worldIn, pos)) //isSimpleFullBlock
            return null;
        return Material2Dye.get(state.getTopMaterialColor(worldIn, pos));
    }
}
