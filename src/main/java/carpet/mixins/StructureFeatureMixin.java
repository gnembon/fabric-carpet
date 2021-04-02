package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.StructureFeatureInterface;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(StructureFeature.class)
public abstract class StructureFeatureMixin<C extends FeatureConfig> implements StructureFeatureInterface<C>
{
    @Shadow public abstract String getName();

    @Shadow public abstract StructureFeature.StructureStartFactory getStructureStartFactory();

    @Shadow protected abstract boolean shouldStartAt(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long worldSeed, ChunkRandom random, ChunkPos chunkPos, Biome biome, ChunkPos chunkPos2, C featureConfig, HeightLimitView heightLimitView);

    @Override
    public boolean plopAnywhere(ServerWorld world, BlockPos pos, ChunkGenerator generator, boolean wireOnly, Biome biome, FeatureConfig config)
    {
        if (world.isClient())
            return false;
        CarpetSettings.skipGenerationChecks = true;
        try
        {
            Random rand = new Random(world.getRandom().nextInt());
            int j = pos.getX() >> 4;
            int k = pos.getZ() >> 4;
            long chId = ChunkPos.toLong(j, k);
            StructureStart structurestart = forceStructureStart(world, generator, rand, chId, biome, config);
            if (structurestart == StructureStart.DEFAULT)
            {
                return false;
            }
            world.getChunk(j, k).addStructureReference((StructureFeature) (Object)this, chId);

            BlockBox box = structurestart.setBoundingBoxFromChildren();  // getBB
            if (!wireOnly)
            {
                structurestart.generateStructure(world, world.getStructureAccessor(), generator, rand,box, new ChunkPos(j, k));
            }
            //structurestart.notifyPostProcessAt(new ChunkPos(j, k));
            int i = Math.max(box.getBlockCountX(),box.getBlockCountZ())/16+1;

            //int i = getRadius();
            for (int k1 = j - i; k1 <= j + i; ++k1)
            {
                for (int l1 = k - i; l1 <= k + i; ++l1)
                {
                    if (k1 == j && l1 == k) continue;
                    long nbchkid = ChunkPos.toLong(k1, l1);
                    if (box.intersectsXZ(k1<<4, l1<<4, (k1<<4) + 15, (l1<<4) + 15))
                    {
                        world.getChunk(k1, l1).addStructureReference((StructureFeature) (Object)this, chId);
                    }
                }
            }
        }
        catch (Exception booboo)
        {
            CarpetSettings.LOG.error("Unknown Exception while plopping structure: "+booboo);
            booboo.printStackTrace();
            return false;
        }
        finally
        {
            CarpetSettings.skipGenerationChecks = false;
        }
        return true;
    }

    private StructureStart forceStructureStart(ServerWorld worldIn, ChunkGenerator generator, Random rand, long packedChunkPos, Biome biome, FeatureConfig config)
    {
        ChunkPos chunkpos = new ChunkPos(packedChunkPos);
        StructureStart structurestart;

        Chunk ichunk = worldIn.getChunk(chunkpos.x, chunkpos.z, ChunkStatus.STRUCTURE_STARTS, false);

        if (ichunk != null)
        {
            structurestart = ichunk.getStructureStart((StructureFeature)(Object)this);

            if (structurestart != null && structurestart != StructureStart.DEFAULT)
            {
                return structurestart;
            }
        }
        Biome biome_1 = biome;
        if (biome == null)
            biome_1 = generator.getBiomeSource().getBiomeForNoiseGen((chunkpos.getStartX() + 9) >> 2, 0, (chunkpos.getStartZ() + 9) >> 2 );

        StructureStart structurestart1 = getStructureStartFactory().create((StructureFeature)(Object)this, chunkpos,0,worldIn.getSeed());
        if (config == null)
            config = new DefaultFeatureConfig();
        structurestart1.init(worldIn.getRegistryManager(), generator, worldIn.getStructureManager() , chunkpos, biome_1, config, ichunk);
        structurestart = structurestart1.hasChildren() ? structurestart1 : StructureStart.DEFAULT;

        if (structurestart.hasChildren())
        {
            worldIn.getChunk(chunkpos.x, chunkpos.z).setStructureStart((StructureFeature)(Object)this, structurestart);
        }

        //long2objectmap.put(packedChunkPos, structurestart);
        return structurestart;
    }

    @Override
    public boolean shouldStartPublicAt(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long l, ChunkRandom chunkRandom, ChunkPos chpos, Biome biome, ChunkPos chunkPos, C featureConfig, HeightLimitView heightLimitView)
    {
        return shouldStartAt(chunkGenerator, biomeSource, l, chunkRandom, chpos, biome, chunkPos, featureConfig, heightLimitView);
    }
}
