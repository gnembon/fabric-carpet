package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.StructureFeatureInterface;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableIntBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(StructureFeature.class)
public abstract class StructureFeatureMixin implements StructureFeatureInterface
{
    //problem is that is seems that now chunks deal with its portion of the strucuture
    //on its own.

    @Shadow public abstract int getRadius();

    @Shadow public abstract String getName();

    @Shadow public abstract StructureFeature.StructureStartFactory getStructureStartFactory();

    @Override
    public boolean plopAnywhere(IWorld world, BlockPos pos)
    {
        return plopAnywhere(world, pos, world.getChunkManager().getChunkGenerator());
    }
    public boolean plopAnywhere(IWorld world, BlockPos pos, ChunkGenerator<? extends ChunkGeneratorConfig> generator)
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
            StructureStart structurestart = forceStructureStart(world, generator, rand, chId);
            if (structurestart == null || structurestart == StructureStart.DEFAULT)
            {
                CarpetSettings.skipGenerationChecks = false;
                return false;
            }
            //generator.ge   getStructurePositionToReferenceMap(this).computeIfAbsent(chId,
            //    (x) -> new LongOpenHashSet()).add(chId);
            world.getChunk(j, k).addStructureReference(this.getName(), chId);  //, ChunkStatus.STRUCTURE_STARTS

            MutableIntBoundingBox box = structurestart.getBoundingBox();
            structurestart.generateStructure(
                    world,
                    rand,
                    new MutableIntBoundingBox(
                            pos.getX() - this.getRadius()*16,
                            pos.getZ() - this.getRadius()*16,
                            pos.getX() + (this.getRadius()+1)*16,
                            pos.getZ() + (1+this.getRadius())*16),
                    new ChunkPos(j, k)
            );
            //structurestart.notifyPostProcessAt(new ChunkPos(j, k));

            int i = getRadius();
            for (int k1 = j - i; k1 <= j + i; ++k1)
            {
                for (int l1 = k - i; l1 <= k + i; ++l1)
                {
                    if (k1 == j && l1 == k) continue;
                    long nbchkid = ChunkPos.toLong(k1, l1);
                    if (box.intersectsXZ(k1<<4, l1<<4, (k1<<4) + 15, (l1<<4) + 15))
                    {
                        //generator.getStructurePositionToReferenceMap(this).computeIfAbsent(nbchkid, (__) -> new LongOpenHashSet()).add(chId);
                        world.getChunk(k1, l1).addStructureReference(this.getName(), chId); //, ChunkStatus.STRUCTURE_STARTS
                        //structurestart.  notifyPostProcessAt(new ChunkPos(k1, l1));
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            CarpetSettings.LOG.error("Unknown Exception while plopping structure: "+ignored);
            ignored.printStackTrace();
            CarpetSettings.skipGenerationChecks = false;
            return false;
        }
        CarpetSettings.skipGenerationChecks = false;
        return true;
    }

    private StructureStart forceStructureStart(IWorld worldIn, ChunkGenerator <? extends ChunkGeneratorConfig > generator, Random rand, long packedChunkPos)
    {
        ChunkPos chunkpos = new ChunkPos(packedChunkPos);
        StructureStart structurestart;

        Chunk ichunk = worldIn.getChunk(chunkpos.x, chunkpos.z, ChunkStatus.STRUCTURE_STARTS);

        if (ichunk != null)
        {
            structurestart = ichunk.getStructureStart(this.getName());

            if (structurestart != null && structurestart != StructureStart.DEFAULT)
            {
                return structurestart;
            }
        }
        Biome biome_1 = generator.getBiomeSource().getBiome(new BlockPos(chunkpos.getStartX() + 9, 0, chunkpos.getStartZ() + 9));
        StructureStart structurestart1 = getStructureStartFactory().create((StructureFeature)(Object)this, chunkpos.x, chunkpos.z, biome_1, MutableIntBoundingBox.empty(),0,generator.getSeed());
        structurestart1.initialize(generator, ((ServerWorld)worldIn).getStructureManager() , chunkpos.x, chunkpos.z, biome_1);
        structurestart = structurestart1.hasChildren() ? structurestart1 : StructureStart.DEFAULT;

        if (structurestart.hasChildren())
        {
            worldIn.getChunk(chunkpos.x, chunkpos.z).setStructureStart(this.getName(), structurestart);
        }

        //long2objectmap.put(packedChunkPos, structurestart);
        return structurestart;
    }
}
