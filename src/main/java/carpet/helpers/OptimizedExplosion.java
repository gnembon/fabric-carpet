package carpet.helpers;
//Author: masa

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import carpet.logging.logHelpers.ExplosionLogHelper;
import carpet.mixins.ExplosionAccessor;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static carpet.script.CarpetEventServer.Event.EXPLOSION_OUTCOME;

public class OptimizedExplosion
{
    private static List<Entity> entitylist;
    private static Vec3 vec3dmem;
    private static long tickmem;
    // For disabling the explosion particles and sound
    public static int explosionSound = 0;

    // masa's optimizations
    private static Object2DoubleOpenHashMap<Pair<Vec3, AABB>> densityCache = new Object2DoubleOpenHashMap<>();
    private static MutablePair<Vec3, AABB> pairMutable = new MutablePair<>();
    private static Object2ObjectOpenHashMap<BlockPos, BlockState> stateCache = new Object2ObjectOpenHashMap<>();
    private static Object2ObjectOpenHashMap<BlockPos, FluidState> fluidCache = new Object2ObjectOpenHashMap<>();
    private static BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
    private static ObjectOpenHashSet<BlockPos> affectedBlockPositionsSet = new ObjectOpenHashSet<>();
    private static boolean firstRay;
    private static boolean rayCalcDone;
    private static ArrayList<Float> chances = new ArrayList<>();
    private static BlockPos blastChanceLocation;

    // Creating entity list for scarpet event
    private static List<Entity> entityList = new ArrayList<>();

    public static void doExplosionA(Explosion e, ExplosionLogHelper eLogger) {
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        
        entityList.clear();
        boolean eventNeeded = EXPLOSION_OUTCOME.isNeeded() && !eAccess.getLevel().isClientSide();
        blastCalc(e);

        if (!CarpetSettings.explosionNoBlockDamage) {
            rayCalcDone = false;
            firstRay = true;
            getAffectedPositionsOnPlaneY(e,  0,  0, 15,  0, 15); // bottom
            getAffectedPositionsOnPlaneY(e, 15,  0, 15,  0, 15); // top
            getAffectedPositionsOnPlaneX(e,  0,  1, 14,  0, 15); // west
            getAffectedPositionsOnPlaneX(e, 15,  1, 14,  0, 15); // east
            getAffectedPositionsOnPlaneZ(e,  0,  1, 14,  1, 14); // north
            getAffectedPositionsOnPlaneZ(e, 15,  1, 14,  1, 14); // south
            stateCache.clear();
            fluidCache.clear();

            e.getToBlow().addAll(affectedBlockPositionsSet);
            affectedBlockPositionsSet.clear();
        }

        float f3 = eAccess.getRadius() * 2.0F;
        int k1 = Mth.floor(eAccess.getX() - (double) f3 - 1.0D);
        int l1 = Mth.floor(eAccess.getX() + (double) f3 + 1.0D);
        int i2 = Mth.floor(eAccess.getY() - (double) f3 - 1.0D);
        int i1 = Mth.floor(eAccess.getY() + (double) f3 + 1.0D);
        int j2 = Mth.floor(eAccess.getZ() - (double) f3 - 1.0D);
        int j1 = Mth.floor(eAccess.getZ() + (double) f3 + 1.0D);
        Vec3 vec3d = new Vec3(eAccess.getX(), eAccess.getY(), eAccess.getZ());

        if (vec3dmem == null || !vec3dmem.equals(vec3d) || tickmem != eAccess.getLevel().getGameTime()) {
            vec3dmem = vec3d;
            tickmem = eAccess.getLevel().getGameTime();
            entitylist = eAccess.getLevel().getEntities(null, new AABB(k1, i2, j2, l1, i1, j1));
            explosionSound = 0;
        }

        explosionSound++;

        Entity explodingEntity = eAccess.getSource();
        for (int k2 = 0; k2 < entitylist.size(); ++k2) {
            Entity entity = entitylist.get(k2);


            if (entity == explodingEntity) {
                // entitylist.remove(k2);
                removeFast(entitylist, k2);
                k2--;
                continue;
            }

            if (entity instanceof PrimedTnt && explodingEntity != null &&
                    entity.getX() == explodingEntity.getX() &&
                    entity.getY() == explodingEntity.getY() &&
                    entity.getZ() == explodingEntity.getZ()) {
                if (eLogger != null) {
                    eLogger.onEntityImpacted(entity, new Vec3(0,-0.9923437498509884d, 0));
                }
                continue;
            }

            if (!entity.ignoreExplosion()) {
                double d12 = Math.sqrt(entity.distanceToSqr(eAccess.getX(), eAccess.getY(), eAccess.getZ())) / (double) f3;

                if (d12 <= 1.0D) {
                    double d5 = entity.getX() - eAccess.getX();
                    // Change in 1.16 snapshots to fix a bug with TNT jumping
                    double d7 = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - eAccess.getY();
                    double d9 = entity.getZ() - eAccess.getZ();
                    double d13 = (double) Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

                    if (d13 != 0.0D) {
                        d5 = d5 / d13;
                        d7 = d7 / d13;
                        d9 = d9 / d13;
                        double density;

                        pairMutable.setLeft(vec3d);
                        pairMutable.setRight(entity.getBoundingBox());
                        density = densityCache.getOrDefault(pairMutable, Double.MAX_VALUE);

                        if (density == Double.MAX_VALUE)
                        {
                            Pair<Vec3, AABB> pair = Pair.of(vec3d, entity.getBoundingBox());
                            density = Explosion.getSeenPercent(vec3d, entity);
                            densityCache.put(pair, density);
                        }

                        // If it is needed, it saves the entity
                        if (eventNeeded) {
                            entityList.add(entity);
                        }

                        double d10 = (1.0D - d12) * density;
                        entity.hurt(e.getDamageSource(),
                                (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f3 + 1.0D)));
                        double d11 = d10;

                        if (entity instanceof LivingEntity) {
                            d11 = ProtectionEnchantment.getExplosionKnockbackAfterDampener((LivingEntity) entity, d10);
                        }

                        if (eLogger != null) {
                            eLogger.onEntityImpacted(entity, new Vec3(d5 * d11, d7 * d11, d9 * d11));
                        }

                        entity.setDeltaMovement(entity.getDeltaMovement().add(d5 * d11, d7 * d11, d9 * d11));

                        if (entity instanceof Player player) {

                            if (!player.isSpectator()
                                    && (!player.isCreative() || !player.getAbilities().flying)) {  //getAbilities
                                e.getHitPlayers().put(player, new Vec3(d5 * d10, d7 * d10, d9 * d10));
                            }
                        }
                    }
                }
            }
        }

        densityCache.clear();
    }

    public static void doExplosionB(Explosion e, boolean spawnParticles)
    {
        ExplosionAccessor eAccess = (ExplosionAccessor) e; 
        Level world = eAccess.getLevel();
        double posX = eAccess.getX();
        double posY = eAccess.getY();
        double posZ = eAccess.getZ();

        // If it is needed, calls scarpet event
        if (EXPLOSION_OUTCOME.isNeeded() && !world.isClientSide()) {
            EXPLOSION_OUTCOME.onExplosion((ServerLevel) world, eAccess.getSource(), e::getIndirectSourceEntity,  eAccess.getX(), eAccess.getY(), eAccess.getZ(), eAccess.getRadius(), eAccess.isFire(), e.getToBlow(), entityList, eAccess.getBlockInteraction());
        }

        boolean damagesTerrain = eAccess.getBlockInteraction() != Explosion.BlockInteraction.KEEP;

        // explosionSound incremented till disabling the explosion particles and sound
        if (explosionSound < 100 || explosionSound % 100 == 0)
        {
            world.playSound(null, posX, posY, posZ, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                    (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F);

            if (spawnParticles)
            {
                if (eAccess.getRadius() >= 2.0F && damagesTerrain)
                {
                    world.addParticle(ParticleTypes.EXPLOSION_EMITTER, posX, posY, posZ, 1.0D, 0.0D, 0.0D);
                }
                else
                {
                    world.addParticle(ParticleTypes.EXPLOSION, posX, posY, posZ, 1.0D, 0.0D, 0.0D);
                }
            }
        }

        if (damagesTerrain)
        {
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList = new ObjectArrayList<>();
            Util.shuffle((ObjectArrayList<BlockPos>) e.getToBlow(), world.random);

            boolean dropFromExplosions = CarpetSettings.xpFromExplosions || e.getIndirectSourceEntity() instanceof Player;

            for (BlockPos blockpos : e.getToBlow())
            {
                BlockState state = world.getBlockState(blockpos);
                Block block = state.getBlock();

                if (state.getMaterial() != Material.AIR)
                {
                    if (block.dropFromExplosion(e) && world instanceof ServerLevel serverLevel)
                    {
                        BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(blockpos) : null;  //hasBlockEntity()

                        LootContext.Builder lootBuilder = (new LootContext.Builder((ServerLevel)eAccess.getLevel()))
                                .withRandom(eAccess.getLevel().random)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos))
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                                .withOptionalParameter(LootContextParams.THIS_ENTITY, eAccess.getSource());

                        if (eAccess.getBlockInteraction() == Explosion.BlockInteraction.DESTROY_WITH_DECAY)
                            lootBuilder.withParameter(LootContextParams.EXPLOSION_RADIUS, eAccess.getRadius());

                        state.spawnAfterBreak(serverLevel, blockpos, ItemStack.EMPTY, dropFromExplosions);

                        state.getDrops(lootBuilder).forEach((itemStackx) -> {
                            method_24023(objectArrayList, itemStackx, blockpos.immutable());
                        });
                    }

                    world.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 3);
                    block.wasExploded(world, blockpos, e);
                }
            }
            objectArrayList.forEach(p -> Block.popResource(world, p.getRight(), p.getLeft()));

        }

        if (eAccess.isFire())
        {
            for (BlockPos blockpos1 : e.getToBlow())
            {
                // Use the same Chunk reference because the positions are in the same xz-column
                ChunkAccess chunk = world.getChunk(blockpos1.getX() >> 4, blockpos1.getZ() >> 4);

                BlockPos down = blockpos1.below(1);
                if (eAccess.getRandom().nextInt(3) == 0 &&
                        chunk.getBlockState(blockpos1).getMaterial() == Material.AIR &&
                        chunk.getBlockState(down).isSolidRender(world, down)
                        )
                {
                    world.setBlockAndUpdate(blockpos1, Blocks.FIRE.defaultBlockState());
                }
            }
        }
    }

    // copied from Explosion, need to move the code to the explosion code anyways and use shadows for
    // simplicity, its not jarmodding anyways
    private static void method_24023(ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList, ItemStack itemStack, BlockPos blockPos) {
        int i = objectArrayList.size();

        for(int j = 0; j < i; ++j) {
            Pair<ItemStack, BlockPos> pair = objectArrayList.get(j);
            ItemStack itemStack2 = pair.getLeft();
            if (ItemEntity.areMergable(itemStack2, itemStack)) {
                ItemStack itemStack3 = ItemEntity.merge(itemStack2, itemStack, 16);
                objectArrayList.set(j, Pair.of(itemStack3, pair.getRight()));
                if (itemStack.isEmpty()) {
                    return;
                }
            }
        }

        objectArrayList.add(Pair.of(itemStack, blockPos));
    }

    private static void removeFast(List<Entity> lst, int index) {
        if (index < lst.size() - 1)
            lst.set(index, lst.get(lst.size() - 1));
        lst.remove(lst.size() - 1);
    }

    private static void rayCalcs(Explosion e) {
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        boolean first = true;

        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d0 = (double) ((float) j / 15.0F * 2.0F - 1.0F);
                        double d1 = (double) ((float) k / 15.0F * 2.0F - 1.0F);
                        double d2 = (double) ((float) l / 15.0F * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 = d0 / d3;
                        d1 = d1 / d3;
                        d2 = d2 / d3;
                        float rand = eAccess.getLevel().random.nextFloat();
                        if (CarpetSettings.tntRandomRange >= 0) {
                            rand = (float) CarpetSettings.tntRandomRange;
                        }
                        float f = eAccess.getRadius() * (0.7F + rand * 0.6F);
                        double d4 = eAccess.getX();
                        double d6 = eAccess.getY();
                        double d8 = eAccess.getZ();

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = new BlockPos(d4, d6, d8);
                            BlockState state = eAccess.getLevel().getBlockState(blockpos);
                            FluidState fluidState = eAccess.getLevel().getFluidState(blockpos);

                            if (state.getMaterial() != Material.AIR) {
                                float f2 = Math.max(state.getBlock().getExplosionResistance(), fluidState.getExplosionResistance());
                                if (eAccess.getSource() != null)
                                    f2 = eAccess.getSource().getBlockExplosionResistance(e, eAccess.getLevel(), blockpos, state, fluidState, f2);
                                f -= (f2 + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && (eAccess.getSource() == null ||
                                    eAccess.getSource().shouldBlockExplode(e, eAccess.getLevel(), blockpos, state, f)))
                            {
                                affectedBlockPositionsSet.add(blockpos);
                            }
                            else if (first) {
                                return;
                            }

                            first = false;

                            d4 += d0 * 0.30000001192092896D;
                            d6 += d1 * 0.30000001192092896D;
                            d8 += d2 * 0.30000001192092896D;
                        }
                    }
                }
            }
        }
    }

    private static void getAffectedPositionsOnPlaneX(Explosion e, int x, int yStart, int yEnd, int zStart, int zEnd)
    {
        if (!rayCalcDone)
        {
            final double xRel = (double) x / 15.0D * 2.0D - 1.0D;

            for (int z = zStart; z <= zEnd; ++z)
            {
                double zRel = (double) z / 15.0D * 2.0D - 1.0D;

                for (int y = yStart; y <= yEnd; ++y)
                {
                    double yRel = (double) y / 15.0D * 2.0D - 1.0D;

                    if (checkAffectedPosition(e, xRel, yRel, zRel))
                    {
                        return;
                    }
                }
            }
        }
    }

    private static void getAffectedPositionsOnPlaneY(Explosion e, int y, int xStart, int xEnd, int zStart, int zEnd)
    {
        if (!rayCalcDone)
        {
            final double yRel = (double) y / 15.0D * 2.0D - 1.0D;

            for (int z = zStart; z <= zEnd; ++z)
            {
                double zRel = (double) z / 15.0D * 2.0D - 1.0D;

                for (int x = xStart; x <= xEnd; ++x)
                {
                    double xRel = (double) x / 15.0D * 2.0D - 1.0D;

                    if (checkAffectedPosition(e, xRel, yRel, zRel))
                    {
                        return;
                    }
                }
            }
        }
    }

    private static void getAffectedPositionsOnPlaneZ(Explosion e, int z, int xStart, int xEnd, int yStart, int yEnd)
    {
        if (!rayCalcDone)
        {
            final double zRel = (double) z / 15.0D * 2.0D - 1.0D;

            for (int x = xStart; x <= xEnd; ++x)
            {
                double xRel = (double) x / 15.0D * 2.0D - 1.0D;

                for (int y = yStart; y <= yEnd; ++y)
                {
                    double yRel = (double) y / 15.0D * 2.0D - 1.0D;

                    if (checkAffectedPosition(e, xRel, yRel, zRel))
                    {
                        return;
                    }
                }
            }
        }
    }

    private static boolean checkAffectedPosition(Explosion e, double xRel, double yRel, double zRel)
    {
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        double len = Math.sqrt(xRel * xRel + yRel * yRel + zRel * zRel);
        double xInc = (xRel / len) * 0.3;
        double yInc = (yRel / len) * 0.3;
        double zInc = (zRel / len) * 0.3;
        float rand = eAccess.getLevel().random.nextFloat();
        float sizeRand = (CarpetSettings.tntRandomRange >= 0 ? (float) CarpetSettings.tntRandomRange : rand);
        float size = eAccess.getRadius() * (0.7F + sizeRand * 0.6F);
        double posX = eAccess.getX();
        double posY = eAccess.getY();
        double posZ = eAccess.getZ();

        for (float f1 = 0.3F; size > 0.0F; size -= 0.22500001F)
        {
            posMutable.set(posX, posY, posZ);

            // Don't query already cached positions again from the world
            BlockState state = stateCache.get(posMutable);
            FluidState fluid = fluidCache.get(posMutable);
            BlockPos posImmutable = null;

            if (state == null)
            {
                posImmutable = posMutable.immutable();
                state = eAccess.getLevel().getBlockState(posImmutable);
                stateCache.put(posImmutable, state);
                fluid = eAccess.getLevel().getFluidState(posImmutable);
                fluidCache.put(posImmutable, fluid);
            }

            if (state.getMaterial() != Material.AIR)
            {
                float resistance = Math.max(state.getBlock().getExplosionResistance(), fluid.getExplosionResistance());

                if (eAccess.getSource() != null)
                {
                    resistance = eAccess.getSource().getBlockExplosionResistance(e, eAccess.getLevel(), posMutable, state, fluid, resistance);
                }

                size -= (resistance + 0.3F) * 0.3F;
            }

            if (size > 0.0F)
            {
                if ((eAccess.getSource() == null || eAccess.getSource().shouldBlockExplode(e, eAccess.getLevel(), posMutable, state, size)))
                    affectedBlockPositionsSet.add(posImmutable != null ? posImmutable : posMutable.immutable());
            }
            else if (firstRay)
            {
                rayCalcDone = true;
                return true;
            }

            firstRay = false;

            posX += xInc;
            posY += yInc;
            posZ += zInc;
        }

        return false;
    }

    public static void setBlastChanceLocation(BlockPos p){
        blastChanceLocation = p;
    }

    private static void blastCalc(Explosion e){
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        if(blastChanceLocation == null || blastChanceLocation.distToLowCornerSqr(eAccess.getX(), eAccess.getY(), eAccess.getZ()) > 200) return;
        chances.clear();
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d0 = (double) ((float) j / 15.0F * 2.0F - 1.0F);
                        double d1 = (double) ((float) k / 15.0F * 2.0F - 1.0F);
                        double d2 = (double) ((float) l / 15.0F * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 = d0 / d3;
                        d1 = d1 / d3;
                        d2 = d2 / d3;
                        float f = eAccess.getRadius() * (0.7F + 0.6F);
                        double d4 = eAccess.getX();
                        double d6 = eAccess.getY();
                        double d8 = eAccess.getZ();
                        boolean found = false;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = new BlockPos(d4, d6, d8);
                            BlockState state = eAccess.getLevel().getBlockState(blockpos);
                            FluidState fluidState = eAccess.getLevel().getFluidState(blockpos);

                            if (state.getMaterial() != Material.AIR) {
                                float f2 = Math.max(state.getBlock().getExplosionResistance(), fluidState.getExplosionResistance());
                                if (eAccess.getSource() != null)
                                    f2 = eAccess.getSource().getBlockExplosionResistance(e, eAccess.getLevel(), blockpos, state, fluidState, f2);
                                f -= (f2 + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && (eAccess.getSource() == null ||
                                    eAccess.getSource().shouldBlockExplode(e, eAccess.getLevel(), blockpos, state, f))) {
                                if(!found && blockpos.equals(blastChanceLocation)){
                                    chances.add(f);
                                    found = true;
                                }
                            }

                            d4 += d0 * 0.30000001192092896D;
                            d6 += d1 * 0.30000001192092896D;
                            d8 += d2 * 0.30000001192092896D;
                        }
                    }
                }
            }
        }

        //showTNTblastChance(e);
    }

    private static void showTNTblastChance(Explosion e){
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        double randMax = 0.6F * eAccess.getRadius();
        double total = 0;
        boolean fullyBlownUp = false;
        boolean first = true;
        int rays = 0;
        for(float f3 : chances){
            rays++;
            double calc = f3 - randMax;
            if(calc > 0) fullyBlownUp = true;
            double chancePerRay = (Math.abs(calc) / randMax);
            if(!fullyBlownUp){
                if(first){
                    first = false;
                    total = chancePerRay;
                }else {
                    total = total * chancePerRay;
                }
            }
        }
        if(fullyBlownUp) total = 0;
        double chance = 1 - total;
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setRoundingMode (RoundingMode.DOWN);
        nf.setMaximumFractionDigits(2);
        for(Player player : eAccess.getLevel().players()){
            Messenger.m(player,"w Pop: ",
                    "c " + nf.format(chance) + " ",
                    "^w Chance for the block to be destroyed by the blast: " + chance,
                    "?" + chance,
                    "w Remain: ",
                    String.format("c %.2f ", total),
                    "^w Chance the block survives the blast: " + total,
                    "?" + total,
                    "w Rays: ",
                    String.format("c %d ", rays),
                    "^w TNT blast rays going through the block",
                    "?" + rays,
                    "w Size: ",
                    String.format("c %.1f ", eAccess.getRadius()),
                    "^w TNT blast size",
                    "?" + eAccess.getRadius(),
                    "w @: ",
                    String.format("c [%.1f %.1f %.1f] ", eAccess.getX(), eAccess.getY(), eAccess.getZ()),
                    "^w TNT blast location X:" + eAccess.getX() + " Y:" + eAccess.getY() + " Z:" + eAccess.getZ(),
                    "?" + eAccess.getX() + " " + eAccess.getY() + " " + eAccess.getZ()
            );
        }
    }
}
