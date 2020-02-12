package carpet.helpers;
//Author: masa

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import carpet.mixins.ExplosionAccessor;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.Block;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class OptimizedExplosion
{
    private static List<Entity> entitylist;
    private static Vec3d vec3dmem;
    private static long tickmem;
    // For disabling the explosion particles and sound
    public static int explosionSound = 0;

    // masa's optimizations
    private static Object2DoubleOpenHashMap<Pair<Vec3d, Box>> densityCache = new Object2DoubleOpenHashMap<>();
    private static MutablePair<Vec3d, Box> pairMutable = new MutablePair<>();
    private static Object2ObjectOpenHashMap<BlockPos, BlockState> stateCache = new Object2ObjectOpenHashMap<>();
    private static Object2ObjectOpenHashMap<BlockPos, FluidState> fluidCache = new Object2ObjectOpenHashMap<>();
    private static BlockPos.Mutable posMutable = new BlockPos.Mutable(0, 0, 0);
    private static ObjectOpenHashSet<BlockPos> affectedBlockPositionsSet = new ObjectOpenHashSet<>();
    private static boolean firstRay;
    private static boolean rayCalcDone;
    private static ArrayList<Float> chances = new ArrayList<>();
    private static BlockPos blastChanceLocation;

    public static void doExplosionA(Explosion e) {
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        
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

            e.getAffectedBlocks().addAll(affectedBlockPositionsSet);
            affectedBlockPositionsSet.clear();
        }

        float f3 = eAccess.getPower() * 2.0F;
        int k1 = MathHelper.floor(eAccess.getX() - (double) f3 - 1.0D);
        int l1 = MathHelper.floor(eAccess.getX() + (double) f3 + 1.0D);
        int i2 = MathHelper.floor(eAccess.getY() - (double) f3 - 1.0D);
        int i1 = MathHelper.floor(eAccess.getY() + (double) f3 + 1.0D);
        int j2 = MathHelper.floor(eAccess.getZ() - (double) f3 - 1.0D);
        int j1 = MathHelper.floor(eAccess.getZ() + (double) f3 + 1.0D);
        Vec3d vec3d = new Vec3d(eAccess.getX(), eAccess.getY(), eAccess.getZ());

        if (vec3dmem == null || !vec3dmem.equals(vec3d) || tickmem != eAccess.getWorld().getTime()) {
            vec3dmem = vec3d;
            tickmem = eAccess.getWorld().getTime();
            entitylist = eAccess.getWorld().getEntities((Entity)null,
                    new Box((double) k1, (double) i2, (double) j2, (double) l1, (double) i1, (double) j1));
            explosionSound = 0;
        }

        explosionSound++;

        for (int k2 = 0; k2 < entitylist.size(); ++k2) {
            Entity entity = entitylist.get(k2);

            if (entity == eAccess.getEntity()) {
                // entitylist.remove(k2);
                removeFast(entitylist, k2);
                k2--;
                continue;
            }

            if (entity instanceof TntEntity &&
                    entity.getX() == eAccess.getEntity().getX() &&
                    entity.getY() == eAccess.getEntity().getY() &&
                    entity.getZ() == eAccess.getEntity().getZ()) {
                continue;
            }

            if (!entity.isImmuneToExplosion()) {
                double d12 = MathHelper.sqrt(entity.squaredDistanceTo(eAccess.getX(), eAccess.getY(), eAccess.getZ())) / (double) f3;

                if (d12 <= 1.0D) {
                    double d5 = entity.getX() - eAccess.getX();
                    double d7 = entity.getY() + (double) entity.getStandingEyeHeight() - eAccess.getY();
                    double d9 = entity.getZ() - eAccess.getZ();
                    double d13 = (double) MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

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
                            Pair<Vec3d, Box> pair = Pair.of(vec3d, entity.getBoundingBox());
                            density = Explosion.getExposure(vec3d, entity);
                            densityCache.put(pair, density);
                        }

                        double d10 = (1.0D - d12) * density;
                        entity.damage(e.getDamageSource(),
                                (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f3 + 1.0D)));
                        double d11 = d10;

                        if (entity instanceof LivingEntity) {
                            d11 = ProtectionEnchantment.transformExplosionKnockback((LivingEntity) entity, d10);
                        }

                        entity.setVelocity(entity.getVelocity().add(d5 * d11, d7 * d11, d9 * d11));

                        if (entity instanceof PlayerEntity) {
                            PlayerEntity player = (PlayerEntity) entity;

                            if (!player.isSpectator()
                                    && (!player.isCreative() || !player.abilities.flying)) {
                                e.getAffectedPlayers().put(player, new Vec3d(d5 * d10, d7 * d10, d9 * d10));
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
        World world = eAccess.getWorld();
        double posX = eAccess.getX();
        double posY = eAccess.getY();
        double posZ = eAccess.getZ();

        boolean damagesTerrain = eAccess.getBlockDestructionType() != Explosion.DestructionType.NONE;

        // explosionSound incremented till disabling the explosion particles and sound
        if (explosionSound < 100 || explosionSound % 100 == 0)
        {
            world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F,
                    (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F);

            if (eAccess.getPower() >= 2.0F && damagesTerrain)
            {
                world.addParticle(ParticleTypes.EXPLOSION_EMITTER, posX, posY, posZ, 1.0D, 0.0D, 0.0D);
            }
            else
            {
                world.addParticle(ParticleTypes.EXPLOSION, posX, posY, posZ, 1.0D, 0.0D, 0.0D);
            }
        }

        if (damagesTerrain)
        {
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList = new ObjectArrayList();
            Collections.shuffle(e.getAffectedBlocks(), world.random);

            for (BlockPos blockpos : e.getAffectedBlocks())
            {
                BlockState state = world.getBlockState(blockpos);
                Block block = state.getBlock();

                if (spawnParticles)
                {
                    double d0 = (double)((float)blockpos.getX() + world.random.nextFloat());
                    double d1 = (double)((float)blockpos.getY() + world.random.nextFloat());
                    double d2 = (double)((float)blockpos.getZ() + world.random.nextFloat());
                    double d3 = d0 - posX;
                    double d4 = d1 - posY;
                    double d5 = d2 - posZ;
                    double d6 = (double)MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                    d3 = d3 / d6;
                    d4 = d4 / d6;
                    d5 = d5 / d6;
                    double d7 = 0.5D / (d6 / (double) eAccess.getPower() + 0.1D);
                    d7 = d7 * (double)(world.random.nextFloat() * world.random.nextFloat() + 0.3F);
                    d3 = d3 * d7;
                    d4 = d4 * d7;
                    d5 = d5 * d7;
                    world.addParticle(ParticleTypes.POOF,
                            (d0 + posX) / 2.0D, (d1 + posY) / 2.0D, (d2 + posZ) / 2.0D, d3, d4, d5);
                    world.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
                }

                if (state.getMaterial() != Material.AIR)
                {
                    if (block.shouldDropItemsOnExplosion(e) && world instanceof ServerWorld)
                    {
                        BlockEntity blockEntity = block.hasBlockEntity() ? world.getBlockEntity(blockpos) : null;

                        LootContext.Builder lootBuilder = new LootContext.Builder((ServerWorld) eAccess.getWorld())
                                .setRandom(eAccess.getWorld().random)
                                .put(LootContextParameters.POSITION, blockpos)
                                .put(LootContextParameters.TOOL, ItemStack.EMPTY)
                                .putNullable(LootContextParameters.BLOCK_ENTITY, blockEntity);

                        if (eAccess.getBlockDestructionType() == Explosion.DestructionType.DESTROY)
                            lootBuilder.put(LootContextParameters.EXPLOSION_RADIUS, eAccess.getPower());

                        state.getDroppedStacks(lootBuilder).forEach((itemStackx) -> {
                            method_24023(objectArrayList, itemStackx, blockpos.toImmutable());
                        });
                    }

                    world.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 3);
                    block.onDestroyedByExplosion(world, blockpos, e);
                }
            }
            objectArrayList.forEach(p -> Block.dropStack(world, p.getRight(), p.getLeft()));

        }

        if (eAccess.isCreateFire())
        {
            for (BlockPos blockpos1 : e.getAffectedBlocks())
            {
                // Use the same Chunk reference because the positions are in the same xz-column
                Chunk chunk = world.getChunk(blockpos1.getX() >> 4, blockpos1.getZ() >> 4);

                BlockPos down = blockpos1.down(1);
                if (chunk.getBlockState(blockpos1).getMaterial() == Material.AIR &&
                        chunk.getBlockState(down).isFullOpaque(world, down) &&
                        eAccess.getRandom().nextInt(3) == 0)
                {
                    world.setBlockState(blockpos1, Blocks.FIRE.getDefaultState());
                }
            }
        }
    }

    // copied from Explosion, need to move the code to the explosion code anyways and use shadows for
    // simplicity, its not jarmodding anyways
    private static void method_24023(ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList, ItemStack itemStack, BlockPos blockPos) {
        int i = objectArrayList.size();

        for(int j = 0; j < i; ++j) {
            Pair<ItemStack, BlockPos> pair = (Pair)objectArrayList.get(j);
            ItemStack itemStack2 = pair.getLeft();
            if (ItemEntity.canMerge(itemStack2, itemStack)) {
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
                        float rand = eAccess.getWorld().random.nextFloat();
                        if (CarpetSettings.tntRandomRange >= 0) {
                            rand = (float) CarpetSettings.tntRandomRange;
                        }
                        float f = eAccess.getPower() * (0.7F + rand * 0.6F);
                        double d4 = eAccess.getX();
                        double d6 = eAccess.getY();
                        double d8 = eAccess.getZ();

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = new BlockPos(d4, d6, d8);
                            BlockState state = eAccess.getWorld().getBlockState(blockpos);
                            FluidState fluidState = eAccess.getWorld().getFluidState(blockpos);

                            if (state.getMaterial() != Material.AIR) {
                                float f2 = Math.max(state.getBlock().getBlastResistance(), fluidState.getBlastResistance());
                                if (eAccess.getEntity() != null)
                                    f2 = eAccess.getEntity().getEffectiveExplosionResistance(e, eAccess.getWorld(), blockpos, state, fluidState, f2);
                                f -= (f2 + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && (eAccess.getEntity() == null ||
                                    eAccess.getEntity().canExplosionDestroyBlock(e, eAccess.getWorld(), blockpos, state, f)))
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
        float rand = eAccess.getWorld().random.nextFloat();
        float sizeRand = (CarpetSettings.tntRandomRange >= 0 ? (float) CarpetSettings.tntRandomRange : rand);
        float size = eAccess.getPower() * (0.7F + sizeRand * 0.6F);
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
                posImmutable = posMutable.toImmutable();
                state = eAccess.getWorld().getBlockState(posImmutable);
                stateCache.put(posImmutable, state);
                fluid = eAccess.getWorld().getFluidState(posImmutable);
                fluidCache.put(posImmutable, fluid);
            }

            if (state.getMaterial() != Material.AIR)
            {
                float resistance = Math.max(state.getBlock().getBlastResistance(), fluid.getBlastResistance());

                if (eAccess.getEntity() != null)
                {
                    resistance = eAccess.getEntity().getEffectiveExplosionResistance(e, eAccess.getWorld(), posMutable, state, fluid, resistance);
                }

                size -= (resistance + 0.3F) * 0.3F;
            }

            if (size > 0.0F && (eAccess.getEntity() == null || eAccess.getEntity().canExplosionDestroyBlock(e, eAccess.getWorld(), posMutable, state, size)))
            {
                affectedBlockPositionsSet.add(posImmutable != null ? posImmutable : posMutable.toImmutable());
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
        if(blastChanceLocation == null || blastChanceLocation.getSquaredDistance(eAccess.getX(), eAccess.getY(), eAccess.getZ(), false) > 200) return;
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
                        float f = eAccess.getPower() * (0.7F + 0.6F);
                        double d4 = eAccess.getX();
                        double d6 = eAccess.getY();
                        double d8 = eAccess.getZ();
                        boolean found = false;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = new BlockPos(d4, d6, d8);
                            BlockState state = eAccess.getWorld().getBlockState(blockpos);
                            FluidState fluidState = eAccess.getWorld().getFluidState(blockpos);

                            if (state.getMaterial() != Material.AIR) {
                                float f2 = Math.max(state.getBlock().getBlastResistance(), fluidState.getBlastResistance());
                                if (eAccess.getEntity() != null)
                                    f2 = eAccess.getEntity().getEffectiveExplosionResistance(e, eAccess.getWorld(), blockpos, state, fluidState, f2);
                                f -= (f2 + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && (eAccess.getEntity() == null ||
                                    eAccess.getEntity().canExplosionDestroyBlock(e, eAccess.getWorld(), blockpos, state, f))) {
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

        showTNTblastChance(e);
    }

    private static void showTNTblastChance(Explosion e){
        ExplosionAccessor eAccess = (ExplosionAccessor) e;
        double randMax = 0.6F * eAccess.getPower();
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
        for(PlayerEntity player : eAccess.getWorld().getPlayers()){
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
                    String.format("c %.1f ", eAccess.getPower()),
                    "^w TNT blast size",
                    "?" + eAccess.getPower(),
                    "w @: ",
                    String.format("c [%.1f %.1f %.1f] ", eAccess.getX(), eAccess.getY(), eAccess.getZ()),
                    "^w TNT blast location X:" + eAccess.getX() + " Y:" + eAccess.getY() + " Z:" + eAccess.getZ(),
                    "?" + eAccess.getX() + " " + eAccess.getY() + " " + eAccess.getZ()
            );
        }
    }
}