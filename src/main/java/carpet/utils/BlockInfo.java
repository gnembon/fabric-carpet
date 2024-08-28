package carpet.utils;

import java.util.ArrayList;
import java.util.List;

import carpet.script.utils.Colors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class BlockInfo
{
    public static List<Component> blockInfo(BlockPos pos, ServerLevel world)
    {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        String metastring = "";
        final Registry<Block> blocks = world.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (net.minecraft.world.level.block.state.properties.Property<?> iproperty : state.getProperties())
        {
            metastring += ", "+iproperty.getName() + '='+state.getValue(iproperty);
        }
        List<Component> lst = new ArrayList<>();
        lst.add(Messenger.s(""));
        lst.add(Messenger.s("====================================="));
        lst.add(Messenger.s(String.format("Block info for %s%s (id %d%s):", blocks.getKey(block),metastring, blocks.getId(block), metastring )));
        lst.add(Messenger.s(String.format(" - Map colour: %s", Colors.mapColourName.get(state.getMapColor(world, pos)))));
        lst.add(Messenger.s(String.format(" - Sound type: %s", Colors.soundName.get(state.getSoundType()))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Full block: %s", state.isCollisionShapeFullBlock(world, pos)))); //  isFullCube() )));
        lst.add(Messenger.s(String.format(" - Normal cube: %s", state.isRedstoneConductor(world, pos)))); //isNormalCube()))); isSimpleFullBlock
        lst.add(Messenger.s(String.format(" - Is liquid: %s", state.is(Blocks.WATER) || state.is(Blocks.LAVA))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Light in: %d, above: %d",
                Math.max(world.getBrightness(LightLayer.BLOCK, pos),world.getBrightness(LightLayer.SKY, pos)) ,
                Math.max(world.getBrightness(LightLayer.BLOCK, pos.above()),world.getBrightness(LightLayer.SKY, pos.above())))));
        lst.add(Messenger.s(String.format(" - Brightness in: %.2f, above: %.2f", world.getLightLevelDependentMagicValue(pos), world.getLightLevelDependentMagicValue(pos.above()))));
        lst.add(Messenger.s(String.format(" - Is opaque: %s", state.isSolid() )));
        //lst.add(Messenger.s(String.format(" - Light opacity: %d", state.getOpacity(world,pos))));
        //lst.add(Messenger.s(String.format(" - Emitted light: %d", state.getLightValue())));
        //lst.add(Messenger.s(String.format(" - Picks neighbour light value: %s", state.useNeighborBrightness(world, pos))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Causes suffocation: %s", state.isSuffocating(world, pos)))); //canSuffocate
        lst.add(Messenger.s(String.format(" - Blocks movement on land: %s", !state.isPathfindable(PathComputationType.LAND))));
        lst.add(Messenger.s(String.format(" - Blocks movement in air: %s", !state.isPathfindable(PathComputationType.AIR))));
        lst.add(Messenger.s(String.format(" - Blocks movement in liquids: %s", !state.isPathfindable(PathComputationType.WATER))));
        lst.add(Messenger.s(String.format(" - Can burn: %s", state.ignitedByLava())));
        lst.add(Messenger.s(String.format(" - Hardness: %.2f", state.getDestroySpeed(world, pos))));
        lst.add(Messenger.s(String.format(" - Blast resistance: %.2f", block.getExplosionResistance())));
        lst.add(Messenger.s(String.format(" - Ticks randomly: %s", state.isRandomlyTicking())));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Can provide power: %s", state.isSignalSource())));
        lst.add(Messenger.s(String.format(" - Strong power level: %d", world.getDirectSignalTo(pos))));
        lst.add(Messenger.s(String.format(" - Redstone power level: %d", world.getBestNeighborSignal(pos))));
        lst.add(Messenger.s(""));
        lst.add(wander_chances(pos.above(), world));

        return lst;
    }

    private static Component wander_chances(BlockPos pos, ServerLevel worldIn)
    {
        PathfinderMob creature = new ZombifiedPiglin(EntityType.ZOMBIFIED_PIGLIN, worldIn);
        creature.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        creature.moveTo(pos, 0.0F, 0.0F);
        RandomStrollGoal wander = new RandomStrollGoal(creature, 0.8D);
        int success = 0;
        for (int i=0; i<1000; i++)
        {

            Vec3 vec = DefaultRandomPos.getPos(creature, 10, 7); // TargetFinder.findTarget(creature, 10, 7);
            if (vec == null)
            {
                continue;
            }
            success++;
        }
        long total_ticks = 0;
        for (int trie=0; trie<1000; trie++)
        {
            int i;
            for (i=1;i<30*20*60; i++) //*60 used to be 5 hours, limited to 30 mins
            {
                if (wander.canUse())
                {
                    break;
                }
            }
            total_ticks += 3*i;
        }
        creature.discard(); // discarded // remove(Entity.RemovalReason.field_26999); // 2nd option - DISCARDED
        long total_time = (total_ticks)/1000/20;
        return Messenger.s(String.format(" - Wander chance above: %.1f%%\n - Average standby above: %s",
                (100.0F*success)/1000,
                ((total_time>5000)?"INFINITY":(total_time +" s"))
        ));
    }
}
