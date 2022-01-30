package carpet.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class PerimeterDiagnostics
{
    public static class Result
    {
        public int liquid;
        public int ground;
        public int specific;
        public List<BlockPos> samples;
        Result()
        {
            samples = new ArrayList<>();
        }
    }
    private MobSpawnSettings.SpawnerData sle;
    private ServerLevel worldServer;
    private MobCategory ctype;
    private Mob el;
    private PerimeterDiagnostics(ServerLevel server, MobCategory ctype, Mob el)
    {
        this.sle = null;
        this.worldServer = server;
        this.ctype = ctype;
        this.el = el;
    }

    public static Result countSpots(ServerLevel worldserver, BlockPos epos, Mob el)
    {
        BlockPos pos;
        //List<BlockPos> samples = new ArrayList<BlockPos>();
        //if (el != null) CarpetSettings.LOG.error(String.format("Got %s to check",el.toString()));
        int eY = epos.getY();
        int eX = epos.getX();
        int eZ = epos.getZ();
        Result result = new Result();

        //int ground_spawns = 0;
        //int liquid_spawns = 0;
        //int specific_spawns = 0;
        boolean add_water = false;
        boolean add_ground = false;
        MobCategory ctype = null;

        if (el != null)
        {
            if (el instanceof WaterAnimal)
            {
                add_water = true;
                ctype = MobCategory.WATER_CREATURE;
            }
            else if (el instanceof AgeableMob)
            {
                add_ground = true;
                ctype = MobCategory.CREATURE;
            }
            else if (el instanceof Enemy)
            {
                add_ground = true;
                ctype = MobCategory.MONSTER;
            }
            else if (el instanceof AmbientCreature)
            {
                ctype = MobCategory.AMBIENT;
            }
        }
        PerimeterDiagnostics diagnostic = new PerimeterDiagnostics(worldserver,ctype,el);
        EntityType type = EntityType.ZOMBIE;
        if (el != null) type = el.getType();
        int minY = worldserver.getMinBuildHeight();
        int maxY = worldserver.getMaxBuildHeight();
        for (int x = -128; x <= 128; ++x)
        {
            for (int z = -128; z <= 128; ++z)
            {
                if (x*x + z*z > 128*128) // cut out a cyllinder first
                {
                    continue;
                }
                for (int y= minY; y < maxY; ++y)
                {
                    if ((Math.abs(y-eY)>128) )
                    {
                        continue;
                    }
                    int distsq = (x)*(x)+(eY-y)*(eY-y)+(z)*(z);
                    if (distsq > 128*128 || distsq < 24*24)
                    {
                        continue;
                    }
                    pos = new BlockPos(eX+x, y, eZ+z);

                    BlockState iblockstate = worldserver.getBlockState(pos);
                    BlockState iblockstate_down = worldserver.getBlockState(pos.below());
                    BlockState iblockstate_up = worldserver.getBlockState(pos.above());

                    if ( iblockstate.getMaterial() == Material.WATER && iblockstate_down.getMaterial() == Material.WATER && !iblockstate_up.isRedstoneConductor(worldserver, pos)) // isSimpleFUllBLock
                    {
                        result.liquid++;
                        if (add_water && diagnostic.check_entity_spawn(pos))
                        {
                            result.specific++;
                            if (result.samples.size() < 10)
                            {
                                result.samples.add(pos);
                            }
                        }
                    }
                    else
                    {
                        if (iblockstate_down.isRedstoneConductor(worldserver, pos)) // isSimpleFUllBLock
                        {
                            Block block = iblockstate_down.getBlock();
                            boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
                            if( flag && NaturalSpawner.isValidEmptySpawnBlock(worldserver, pos, iblockstate, iblockstate.getFluidState(), type) && NaturalSpawner.isValidEmptySpawnBlock(worldserver, pos.above(), iblockstate_up, iblockstate_up.getFluidState(), type))
                            {
                                result.ground ++;
                                if (add_ground && diagnostic.check_entity_spawn(pos))
                                {
                                    result.specific++;
                                    if (result.samples.size() < 10)
                                    {
                                        result.samples.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //ashMap<String,Integer> result= new HashMap<>();
        //result.put("Potential in-water spawning spaces", liquid_spawns);
        //result.put("Potential on-ground spawning spaces", ground_spawns);
        //if (el != null) result.put(String.format("%s spawning spaces",el.getDisplayName().getUnformattedText()),specific_spawns);
        return result;
    }


    private boolean check_entity_spawn(BlockPos pos)
    {
        if (sle == null || !worldServer.getChunkSource().getGenerator().getMobsAt(worldServer.getBiome(pos), worldServer.structureFeatureManager(), ctype, pos).unwrap().contains(sle))
        {
            sle = null;
            for (MobSpawnSettings.SpawnerData sle: worldServer.getChunkSource().getGenerator().getMobsAt(worldServer.getBiome(pos), worldServer.structureFeatureManager(), ctype, pos).unwrap())
            {
                if (el.getType() == sle.type)
                {
                    this.sle = sle;
                    break;
                }
            }
            if (sle == null || !worldServer.getChunkSource().getGenerator().getMobsAt(worldServer.getBiome(pos), worldServer.structureFeatureManager(), ctype, pos).unwrap().contains(sle))
            {
                return false;
            }
        }

        SpawnPlacements.Type  spt = SpawnPlacements.getPlacementType(sle.type);

        if (NaturalSpawner.isSpawnPositionOk(spt, worldServer, pos, sle.type))
        {
            el.moveTo((float)pos.getX() + 0.5F, (float)pos.getY(), (float)pos.getZ()+0.5F, 0.0F, 0.0F);
            return el.checkSpawnObstruction(worldServer) && el.checkSpawnRules(worldServer, MobSpawnType.NATURAL) &&
                    SpawnPlacements.checkSpawnRules(el.getType(),(ServerLevel)el.getCommandSenderWorld(), MobSpawnType.NATURAL, el.blockPosition(), el.getCommandSenderWorld().random) &&
                    worldServer.noCollision(el); // check collision rules once they stop fiddling with them after 1.14.1
        }
        return false;
    }
}
