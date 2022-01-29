package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.StructureFeatureInterface;
import com.google.common.collect.ImmutableMultimap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

@Mixin(StructureFeature.class)
public abstract class StructureFeatureMixin<C extends FeatureConfiguration> implements StructureFeatureInterface<C>
{
    @Shadow @Final private PieceGeneratorSupplier<C> pieceGenerator;

    @Override
    public boolean plopAnywhere(ServerLevel world, BlockPos pos, ChunkGenerator generator, boolean wireOnly, Biome biome, C config)
    {
        if (world.isClientSide())
            return false;
        CarpetSettings.skipGenerationChecks.set(true);
        try
        {
            Random rand = new Random(world.getRandom().nextInt());
            int j = pos.getX() >> 4;
            int k = pos.getZ() >> 4;
            long chId = ChunkPos.asLong(j, k);
            StructureStart<C> structurestart = forceStructureStart(world, generator, rand, chId, biome, config);
            if (structurestart == StructureStart.INVALID_START)
            {
                return false;
            }
            StructureFeature<C> thiss = (StructureFeature<C>) (Object)this;
            world.getChunk(j, k).addReferenceForFeature(thiss, chId);

            BoundingBox box = structurestart.getBoundingBox();

            if (!wireOnly)
            {
                Registry<StructureFeature<?>> registry3 = world.registryAccess().registryOrThrow(Registry.STRUCTURE_FEATURE_REGISTRY);
                world.setCurrentlyGenerating(() -> {
                    Objects.requireNonNull(thiss);
                    return registry3.getResourceKey(thiss).map(Object::toString).orElseGet(thiss::toString);
                });
                structurestart.placeInChunk(world, world.structureFeatureManager(), generator, rand, box, new ChunkPos(j, k));
            }
            //structurestart.notifyPostProcessAt(new ChunkPos(j, k));
            int i = Math.max(box.getXSpan(),box.getZSpan())/16+1;

            //int i = getRadius();
            for (int k1 = j - i; k1 <= j + i; ++k1)
            {
                for (int l1 = k - i; l1 <= k + i; ++l1)
                {
                    if (k1 == j && l1 == k) continue;
                    long nbchkid = ChunkPos.asLong(k1, l1);
                    if (box.intersects(k1<<4, l1<<4, (k1<<4) + 15, (l1<<4) + 15))
                    {
                        world.getChunk(k1, l1).addReferenceForFeature(thiss, chId);
                    }
                }
            }
        }
        catch (Exception booboo)
        {
            CarpetSettings.LOG.error("Unknown Exception while plopping structure: "+booboo, booboo);
            return false;
        }
        finally
        {
            CarpetSettings.skipGenerationChecks.set(false);
        }
        return true;
    }

    private StructureStart<C> forceStructureStart(ServerLevel worldIn, ChunkGenerator generator, Random rand, long packedChunkPos, Biome biome, C config)
    {
        ChunkPos chunkpos = new ChunkPos(packedChunkPos);
        StructureFeature<C> thiss= (StructureFeature<C>)(Object)this;

        ChunkAccess ichunk = worldIn.getChunk(chunkpos.x, chunkpos.z, ChunkStatus.STRUCTURE_STARTS, false);

        if (ichunk != null)
        {
            StructureStart<?> structurestartt = ichunk.getStartForFeature(thiss);

            if (structurestartt != null && structurestartt != StructureStart.INVALID_START)
            {
                return (StructureStart<C>) structurestartt;
            }
        }
        Optional<PieceGenerator<C>> optional = pieceGenerator.createGenerator(new PieceGeneratorSupplier.Context<>(generator, generator.getBiomeSource(), worldIn.getSeed(), chunkpos, config, worldIn, b -> true, worldIn.getStructureManager(), worldIn.registryAccess()));
        if (optional.isEmpty()) return (StructureStart<C>) StructureStart.INVALID_START;
        StructurePiecesBuilder lv = new StructurePiecesBuilder();
        optional.get().generatePieces(lv, new PieceGenerator.Context<C>(config, generator, worldIn.getStructureManager(), chunkpos, worldIn, Util.make(new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier())), (chunkRandomx) -> {
            chunkRandomx.setLargeFeatureSeed(worldIn.getSeed(), chunkpos.x, chunkpos.z);
        }), worldIn.getSeed()));
        StructureStart<C> structurestart1 = new StructureStart<>(thiss, chunkpos, 0, lv.build());

        if (structurestart1.isValid())
        {
            worldIn.getChunk(chunkpos.x, chunkpos.z).setStartForFeature(thiss, structurestart1);
        }
        return structurestart1;
    }
}
