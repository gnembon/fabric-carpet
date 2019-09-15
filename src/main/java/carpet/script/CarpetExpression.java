package carpet.script;

import carpet.CarpetServer;
import carpet.fakes.MinecraftServerInterface;
import carpet.helpers.FeatureGenerator;
import carpet.script.Fluff.TriFunction;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.settings.CarpetSettings;
import carpet.utils.BlockInfo;
import carpet.utils.Messenger;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.BedrockBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlacementEnvironment;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.packet.PlaySoundIdS2CPacket;
import net.minecraft.command.arguments.ItemStackArgument;
import net.minecraft.command.arguments.ParticleArgumentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.Property;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Clearable;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.loot.context.LootContext;
import net.minecraft.world.loot.context.LootContextParameters;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

/**
 * <h1>Minecraft specific API and <code>scarpet</code> language add-ons and commands</h1>
 * <p>Here is the gist of the Minecraft related functions. Otherwise the CarpetScript could live without Minecraft.</p>
 * <h2>Dimension issues</h2>
 * <p>One note, which is important is that most of the calls for entities and blocks
 * would refer to the current dimension of the caller, meaning, that if we for example
 * list all the players using <code>player('all')</code> function, if a player is in the
 * other dimension, calls to entities and blocks around that player would point incorrectly.
 * Moreover, running commandblocks in the spawn chunks would mean that commands will always
 * refer to the overworld blocks and entities.
 * In case you would want to run commands across all dimensions, just run three of them, using
 * <code>/execute in overworld/the_nether/the_end run script run ...</code> and query
 * players using <code>player('*')</code>, which only returns players in current dimension.</p>
 */
public class CarpetExpression 
{

    private ServerCommandSource source;
    private BlockPos origin;
    private Expression expr;
    Expression getExpr() {return expr;}
    private static long tickStart = 0L;

    private static boolean stopAll = false;

    /**
     * <h1><code>script stop/script resume</code> command</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>
     * <code>/script stop</code> allows to stop execution of any script currently running that calls the
     * <code>gametick()</code> function which
     * allows the game loop to regain control of the game and process other commands. This will also make sure
     * that all current and future programs will stop their execution. Execution of all programs will be
     * prevented until <code>/script resume</code> command is called.
     * </p>
     * <p>Lets look at the following example. This is a program computes Fibonacci number in a recursive manner:</p>
     * <pre>
     * fib(n) -&gt; if(n&lt;3, 1, fib(n-1)+fib(n-2) ); fib(8)
     * </pre>
     * <p> That's really bad way of doing it, because the higher number we need to compute the compute requirements will rise
     * exponentially with <code>n</code>. It takes a little over 50 milliseconds to do fib(24), so above one tick,
     * but about a minute to do fib(40). Calling fib(40) will not only freeze the game, but also you woudn't be able to interrupt
     * its execution. We can modify the script as follows</p>
     * <pre>fib(n) -&gt; ( gametick(50); if(n&lt;3, 1, fib(n-1)+fib(n-2) ) ); fib(40)</pre>
     * <p>But this would never finish as such call would finish after <code>~ 2^40</code> ticks. To make our computations
     * responsive, yet able to respond to user interactions, other commands, as well as interrupt execution,
     * we could do the following:</p>
     * <pre>fib(n) -&gt; ( if(n==23, gametick(50) ); if(n&lt;3, 1, fib(n-1)+fib(n-2) ) ); fib(40)</pre>
     * <p>This would slow down the computation of fib(40) from a minute to two, but allows the game to keep continue running
     * and be responsive to commands, using about half of each tick to advance the computation.
     * Obviously depending on the problem, and available hardware, certain things can take
     * more or less time to execute, so portioning of work with calling <code>gametick</code> should be balanced in each
     * case separately</p>
     * </div>
     * @param doStop .
     */
    public void BreakExecutionOfAllScriptsWithCommands(boolean doStop)
    {
        //unused - accessed via CarpetScriptServer
    }

    private LazyValue booleanStateTest(
            Context c,
            String name,
            List<LazyValue> params,
            BiFunction<BlockState, BlockPos, Boolean> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException(name + " requires at least one parameter");
        }
        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
        {
            Value retval = test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos()) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        }
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
        Value retval = test.apply(block.getBlockState(), block.getPos()) ? Value.TRUE : Value.FALSE;
        return (c_, t_) -> retval;
    }

    private LazyValue stateStringQuery(
            Context c,
            String name,
            List<LazyValue> params,
            BiFunction<BlockState, BlockPos, String> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException(name + " requires at least one parameter");
        }

        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
        {
            Value retval = new StringValue(test.apply( ((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos()));
            return (c_, t_) -> retval;
        }
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
        Value retval = new StringValue(test.apply(block.getBlockState(), block.getPos()));
        return (c_, t_) -> retval;
    }

    private LazyValue genericStateTest(
            Context c,
            String name,
            List<LazyValue> params,
            TriFunction<BlockState, BlockPos, World, Value> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException(name + " requires at least one parameter");
        }
        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
        {
            try
            {
                Value retval = test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos(), cc.s.getWorld());
                return (_c, _t) -> retval;
            }
            catch (NullPointerException ignored)
            {
                throw new InternalExpressionException(name + " function requires a block that is positioned in the world");
            }
        }
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
        Value retval = test.apply(block.getBlockState(), block.getPos(), cc.s.getWorld());
        return (c_, t_) -> retval;
    }

    private <T extends Comparable<T>> BlockState setProperty(Property<T> property, String name, String value,
                                                              BlockState bs)
    {
        Optional<T> optional = property.getValue(value);

        if (optional.isPresent())
        {
            bs = bs.with(property, optional.get());
        }
        else
        {
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        }
        return bs;
    }

    private static Map<String, ParticleEffect> particleCache = new HashMap<>();
    private ParticleEffect getParticleData(String name)
    {
        ParticleEffect particle = particleCache.get(name);
        if (particle != null)
            return particle;
        try
        {
            particle = ParticleArgumentType.readParameters(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("No such particle: "+name);
        }
        particleCache.put(name, particle);
        return particle;
    }

    private int drawParticleLine(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        double lineLengthSq = from.squaredDistanceTo(to);
        if (lineLengthSq == 0) return 0;
        Vec3d incvec = to.subtract(from).multiply(2*density/sqrt(lineLengthSq));
        int pcount = 0;
        for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
             delta.lengthSquared()<lineLengthSq;
             delta = delta.add(incvec.multiply(Expression.randomizer.nextFloat())))
        {
            for (PlayerEntity player : world.getPlayers())
            {
                world.spawnParticles((ServerPlayerEntity)player, particle, true,
                        delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                        0.0, 0.0, 0.0, 0.0);
                pcount ++;
            }
        }
        return pcount;
    }

    private void forceChunkUpdate(BlockPos pos, ServerWorld world)
    {
        Chunk chunk = world.getChunk(pos);
        chunk.setShouldSave(true);
        for (int i = 0; i<16; i++)
        {
            BlockPos section = new BlockPos((pos.getX()>>4 <<4)+8, 16*i, (pos.getZ()>>4 <<4)+8);
            world.method_14178().markForUpdate(section);
            world.method_14178().markForUpdate(section.east());
            world.method_14178().markForUpdate(section.west());
            world.method_14178().markForUpdate(section.north());
        }
    }

    private boolean tryBreakBlock_copy_from_ServerPlayerInteractionManager(ServerPlayerEntity player, BlockPos blockPos_1)
    {
        //this could be done little better, by hooking up event handling not in try_break_block but wherever its called
        // so we can safely call it here
        // but that woudl do for now.
        BlockState blockState_1 = player.world.getBlockState(blockPos_1);
        if (!player.getMainHandStack().getItem().canMine(blockState_1, player.world, blockPos_1, player)) {
            return false;
        } else {
            BlockEntity blockEntity_1 = player.world.getBlockEntity(blockPos_1);
            Block block_1 = blockState_1.getBlock();
            if ((block_1 instanceof CommandBlock || block_1 instanceof StructureBlock || block_1 instanceof JigsawBlock) && !player.isCreativeLevelTwoOp()) {
                player.world.updateListeners(blockPos_1, blockState_1, blockState_1, 3);
                return false;
            } else if (player.method_21701(player.world, blockPos_1, player.interactionManager.getGameMode())) {
                return false;
            } else {
                block_1.onBreak(player.world, blockPos_1, blockState_1, player);
                boolean boolean_1 = player.world.clearBlockState(blockPos_1, false);
                if (boolean_1) {
                    block_1.onBroken(player.world, blockPos_1, blockState_1);
                }

                if (player.isCreative()) {
                    return true;
                } else {
                    ItemStack itemStack_1 = player.getMainHandStack();
                    boolean boolean_2 = player.isUsingEffectiveTool(blockState_1);
                    itemStack_1.postMine(player.world, blockState_1, blockPos_1, player);
                    if (boolean_1 && boolean_2) {
                        ItemStack itemStack_2 = itemStack_1.isEmpty() ? ItemStack.EMPTY : itemStack_1.copy();
                        block_1.afterBreak(player.world, player, blockPos_1, blockState_1, blockEntity_1, itemStack_2);
                    }

                    return true;
                }
            }
        }
    }

    /**
     * <h1>Blocks / World API</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Specifying blocks</h2>
     * <h3><code>block(x, y, z), block(state)</code></h3>
     * <p>Returns either a block from specified location, or block with a specific state
     * (as used by <code>/setblock</code> command), so allowing for block properties, block entity data etc.
     * Blocks otherwise can be referenced everywhere by its simple string name, but its only used in its default state</p>
     * <pre>
     * block('air')  =&gt; air
     * block('iron_trapdoor[half=top]')  =&gt; iron_trapdoor
     * block(0,0,0) == block('bedrock')  =&gt; 1
     * block('hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') =&gt; hopper
     * </pre>
     * <h2>World Manipulation</h2>
     * <p>All the functions below can be used with block value, queried with coord triple, or 3-long list.
     * All <code>pos</code> in the functions referenced below refer to either method of passing block position</p>
     * <h3><code>set(pos, block, property?, value?, ...)</code></h3>
     * <p>First part of the <code>set</code> function is either a coord triple, list of three numbers, or other block
     * with coordinates. Second part, <code>block</code> is either block value as a result of <code>block()</code> function
     * string value indicating the block name, and optional <code>property - value</code> pairs for extra block properties.
     * If <code>block</code> is specified only by name, then if a destination block is the same the <code>set</code> operation
     * is skipped, otherwise is executed, for other potential extra properties</p>
     * <p>The returned value is either the block state that has been set, or <code>false</code> if block setting was skipped</p>
     * <pre>
     * set(0,5,0,'bedrock')  =&gt; bedrock
     * set(l(0,5,0), 'bedrock')  =&gt; bedrock
     * set(block(0,5,0), 'bedrock')  =&gt; bedrock
     * scan(0,5,0,0,0,0,set(_,'bedrock'))  =&gt; 1
     * set(pos(players()), 'bedrock')  =&gt; bedrock
     * set(0,0,0,'bedrock')  =&gt; 0   // or 1 in overworlds generated in 1.8 and before
     * scan(0,100,0,20,20,20,set(_,'glass'))
     *     // filling the area with glass
     * scan(0,100,0,20,20,20,set(_,block('glass')))
     *     // little bit faster due to internal caching of block state selectors
     * b = block('glass'); scan(0,100,0,20,20,20,set(_,b))
     *     // yet another option, skips all parsing
     * set(x,y,z,'iron_trapdoor')  // sets bottom iron trapdoor
     * set(x,y,z,'iron_trapdoor[half=top]')  // Incorrect. sets bottom iron trapdoor - no parsing of properties
     * set(x,y,z,'iron_trapdoor','half','top') // correct - top trapdoor
     * set(x,y,z,block('iron_trapdoor[half=top]')) // also correct, block() provides extra parsing
     * set(x,y,z,'hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') // extra block data
     * </pre>
     * <h3><code>place_item(item, pos, facing?, sneak?)</code></h3>
     * <p>Places a given item in the world like it was placed by a player. Item names are default minecraft item name,
     * less the minecraft prefix. Default facing is 'up', but there are other options: 'down', 'north', 'east',
     * 'south', 'west', but also there are other secondary directions important for placement of blocks like stairs, doors, etc.
     * Try experiment with options like 'north-up' which is placed facing north with cursor pointing to the upper part of the block,
     * or 'up-north', which means a block placed facing up (player looking down) and placed smidge away of the block center
     * towards north. Optional sneak is a boolean indicating if a player would be sneaking while placing the block -
     * this option only affects placement of chests and scaffolding at the moment.
     * Returns true if placement was successful, false otherwise.</p>
     * <pre>
     *     place_item('stone',x,y,z) // places a stone block on x,y,z block
     *     place_item('piston,x,y,z,'down') // places a piston facing down
     *     place_item('carrot',x,y,z) // attempts to plant a carrot plant. Returns true if could place carrots at that position.
     * </pre>
     * <h3><code>set_biome(pos, biome_name)</code></h3>
     * <p>changes biome at that block position.</p>
     * <h3><code>update(pos)</code></h3>
     * <p>Causes a block update at position.</p>
     * <h3><code>block_tick(pos)</code></h3>
     * <p>Causes a block to tick at position.</p>
     * <h3><code>random_tick(pos)</code></h3>
     * <p>Causes a random tick at position.</p>
     * <h3><code>destroy(pos), destroy(pos, -1), destroy(pos, &lt;N&gt;)</code></h3>
     * <p>Destroys the block like it was mined by a player. Add -1 for silk touch, and positive number for fortune level.
     * This function, unlike harvest, will affect all kinds of blocks</p>
     * <h3><code>harvest(player, pos)</code></h3>
     * <p>Causes a block to be harvested by a specified player entity. Honors player item enchantments, as well as damages the
     * tool if applicable. If the entity is not a valid player, no block gets destroyed. If a player is not allowed to
     * break that block, a block doesn't get destroyed either.</p>
     *
     * <h2>Block and World querying</h2>
     *
     * <h3><code>pos(block), pos(entity)</code></h3>
     * <p>Returns a triple of coordinates of a specified block or entity. Technically entities are queried with
     * <code>query</code> function and the same can be achieved with <code>query(entity,'pos')</code>, but for simplicity
     * <code>pos</code> allows to pass all positional objects.</p>
     * <pre>
     *     pos(block(0,5,0))  =&gt; l(0,5,0)
     *     pos(players()) =&gt; l(12.3, 45.6, 32.05)
     *     pos(block('stone'))  =&gt; Error: Cannot fetch position of an unrealized block
     * </pre>
     * <h3><code>block_properties(pos)</code></h3>
     * <p>Returns a list of available block properties for a particular block. If a block has no properties, returns an empty list.</p>
     * <h3><code>property(pos, name)</code></h3>
     * <p>Returns property of block at <code>pos</code>, or specified by <code>block</code> argument. If a block doesn't
     * have that property, <code>null</code> value is returned. Returned values are always strings. It is expected from
     * the user to know what to expect and convert values to numbers using <code>number()</code> function or booleans
     * using <code>bool()</code> function.</p>
     * <pre>
     *     set(x,y,z,'iron_trapdoor','half','top'); property(x,y,z,'half')  =&gt; top
     *     set(x,y,z,'air'); property(x,y,z,'half')  =&gt; null
     *     property(block('iron_trapdoor[half=top]'),'half')  =&gt; top
     *     property(block('iron_trapdoor[half=top]'),'powered')  =&gt; false
     *     bool(property(block('iron_trapdoor[half=top]'),'powered'))  =&gt; 0
     * </pre>
     * <h3><code>block_data(pos)</code></h3>
     * <p>Return NBT string associated with specific location, or null if the block does not carry block data.
     * Can be currently used to match specific information from it, or use it to copy to another block</p>
     * <pre>
     *     block_data(x,y,z) =&gt; '{TransferCooldown:0,x:450,y:68, ... }'
     * </pre>
     * <h3><code>solid(pos)</code></h3>
     * <p>Boolean function, true if the block is solid</p>
     * <h3> <code>air(pos)</code></h3>
     * <p>Boolean function, true if a block is air.... or cave air...
     * or void air.... or any other air they come up with.</p>
     * <h3><code>liquid(pos)</code></h3>
     * <p>Boolean function, true if the block is liquid, or liquidlogged</p>
     * <h3><code>flammable(pos)</code></h3>
     * <p>Boolean function, true if the block is flammable</p>
     * <h3><code>transparent(pos)</code></h3>
     * <p>Boolean function, true if the block is transparent</p>
     * <h3><code>opacity(pos)</code></h3>
     * <p>Numeric, returning opacity level of a block</p>
     * <h3><code>blocks_daylight(pos)</code></h3>
     * <p>Boolean function, true if the block blocks daylight</p>
     * <h3><code>emitted_light(pos)</code></h3>
     * <p>Numeric, returning light level emitted from block</p>
     * <h3><code>light(pos)</code></h3>
     * <p>Integer function, returning total light level at position</p>
     * <h3><code>block_light(pos)</code></h3>
     * <p>Integer function, returning block light at position. From torches and other light sources.</p>
     * <h3><code>sky_light(pos)</code></h3>
     * <p>Numeric function, returning sky light at position. From the sky access.</p>
     * <h3><code>see_sky(pos)</code></h3>
     * <p>Boolean function, returning true if the block can see sky.</p>
     * <h3><code>hardness(pos)</code></h3>
     * <p>Numeric function, indicating hardness of a block.</p>
     * <h3><code>blast_resistance(pos)</code></h3>
     * <p>Numeric function, indicating blast_resistance of a block.</p>

     * <h3><code>top(type, pos)</code></h3>
     * <p>Returns the Y value of the topmost block at given x, z coords (y value of a block is not important), according to the
     * heightmap specified by <code>type</code>. Valid options are:</p>
     * <ul>
     *     <li><code>light</code>: topmost light blocking block (1.13 only)</li>
     *     <li><code>motion</code>: topmost motion blocking block</li>
     *     <li><code>terrain</code>: topmost motion blocking block except leaves</li>
     *     <li><code>ocean_floor</code>: topmost non-water block</li>
     *     <li><code>surface</code>: topmost surface block</li>
     * </ul>
     * <pre>
     * top('motion', x, y, z)  =&gt; 63
     * top('ocean_floor', x, y, z)  =&gt; 41
     * </pre>
     * <h3><code>loaded(pos)</code></h3>
     * <p>Boolean function, true if the block is loaded. Normally <code>scarpet</code> doesn't check if operates on
     * loaded area - the game will automatically load missing blocks. We see this as advantage.
     * Vanilla <code>fill/clone</code> commands only check the specified corners for loadness.</p>
     * <pre>
     * loaded(pos(players()))  =&gt; 1
     * loaded(100000,100,1000000)  =&gt; 0
     * </pre>
     * <h3><code>loaded_ep(pos)</code></h3>
     * <p>Boolean function, true if the block is loaded and entity processing, as per 1.13.2</p>
     * <h3><code>suffocates(pos)</code></h3>
     * <p>Boolean function, true if the block causes suffocation.</p>
     * <h3><code>power(pos)</code></h3>
     * <p>Numeric function, returning redstone power level at position.</p>
     * <h3><code>ticks_randomly(pos)</code></h3>
     * <p>Boolean function, true if the block ticks randomly.</p>
     * <h3><code>blocks_movement(pos)</code></h3>
     * <p>Boolean function, true if block at position blocks movement.</p>
     * <h3><code>block_sound(pos)</code></h3>
     * <p>Returns the name of sound type made by the block at position. One of:</p>
     * <ul>
     *     <li><code>wood     </code>  </li>
     *     <li><code>gravel   </code>  </li>
     *     <li><code>grass    </code>  </li>
     *     <li><code>stone    </code>  </li>
     *     <li><code>metal    </code>  </li>
     *     <li><code>glass    </code>  </li>
     *     <li><code>wool     </code>  </li>
     *     <li><code>sand     </code>  </li>
     *     <li><code>snow     </code>  </li>
     *     <li><code>ladder   </code>  </li>
     *     <li><code>anvil    </code>  </li>
     *     <li><code>slime    </code>  </li>
     *     <li><code>sea_grass</code>  </li>
     *     <li><code>coral    </code>  </li>
     * </ul>
     * <h3><code>material(pos)</code></h3>
     * <p>Returns the name of material of the block at position. very useful to target a group of blocks. One of:</p>
     * <ul>
     *     <li><code> air                </code>  </li>
     *     <li><code> void               </code>  </li>
     *     <li><code> portal             </code>  </li>
     *     <li><code> carpet             </code>  </li>
     *     <li><code> plant              </code>  </li>
     *     <li><code> water_plant        </code>  </li>
     *     <li><code> vine               </code>  </li>
     *     <li><code> sea_grass          </code>  </li>
     *     <li><code> water              </code>  </li>
     *     <li><code> bubble_column      </code>  </li>
     *     <li><code> lava               </code>  </li>
     *     <li><code> snow_layer         </code>  </li>
     *     <li><code> fire               </code>  </li>
     *     <li><code> redstone_bits      </code>  </li>
     *     <li><code> cobweb             </code>  </li>
     *     <li><code> redstone_lamp      </code>  </li>
     *     <li><code> clay               </code>  </li>
     *     <li><code> dirt               </code>  </li>
     *     <li><code> grass              </code>  </li>
     *     <li><code> packed_ice         </code>  </li>
     *     <li><code> sand               </code>  </li>
     *     <li><code> sponge             </code>  </li>
     *     <li><code> wood               </code>  </li>
     *     <li><code> wool               </code>  </li>
     *     <li><code> tnt                </code>  </li>
     *     <li><code> leaves             </code>  </li>
     *     <li><code> glass              </code>  </li>
     *     <li><code> ice                </code>  </li>
     *     <li><code> cactus             </code>  </li>
     *     <li><code> stone              </code>  </li>
     *     <li><code> iron               </code>  </li>
     *     <li><code> snow               </code>  </li>
     *     <li><code> anvil              </code>  </li>
     *     <li><code> barrier            </code>  </li>
     *     <li><code> piston             </code>  </li>
     *     <li><code> coral              </code>  </li>
     *     <li><code> gourd              </code>  </li>
     *     <li><code> dragon_egg         </code>  </li>
     *     <li><code> cake               </code>  </li>
     * </ul>
     * <h3><code>map_colour(pos)</code></h3>
     * <p>Returns the map colour of a block at position. One of:</p>
     * <ul>
     *     <li><code> air            </code>  </li>
     *     <li><code> grass          </code>  </li>
     *     <li><code> sand           </code>  </li>
     *     <li><code> wool           </code>  </li>
     *     <li><code> tnt            </code>  </li>
     *     <li><code> ice            </code>  </li>
     *     <li><code> iron           </code>  </li>
     *     <li><code> foliage        </code>  </li>
     *     <li><code> snow           </code>  </li>
     *     <li><code> clay           </code>  </li>
     *     <li><code> dirt           </code>  </li>
     *     <li><code> stone          </code>  </li>
     *     <li><code> water          </code>  </li>
     *     <li><code> wood           </code>  </li>
     *     <li><code> quartz         </code>  </li>
     *     <li><code> adobe          </code>  </li>
     *     <li><code> magenta        </code>  </li>
     *     <li><code> light_blue     </code>  </li>
     *     <li><code> yellow         </code>  </li>
     *     <li><code> lime           </code>  </li>
     *     <li><code> pink           </code>  </li>
     *     <li><code> gray           </code>  </li>
     *     <li><code> light_gray     </code>  </li>
     *     <li><code> cyan           </code>  </li>
     *     <li><code> purple         </code>  </li>
     *     <li><code> blue           </code>  </li>
     *     <li><code> brown          </code>  </li>
     *     <li><code> green          </code>  </li>
     *     <li><code> red            </code>  </li>
     *     <li><code> black          </code>  </li>
     *     <li><code> gold           </code>  </li>
     *     <li><code> diamond        </code>  </li>
     *     <li><code> lapis          </code>  </li>
     *     <li><code> emerald        </code>  </li>
     *     <li><code> obsidian       </code>  </li>
     *     <li><code> netherrack     </code>  </li>
     *     <li><code> white_terracotta          </code>  </li>
     *     <li><code> orange_terracotta         </code>  </li>
     *     <li><code> magenta_terracotta        </code>  </li>
     *     <li><code> light_blue_terracotta     </code>  </li>
     *     <li><code> yellow_terracotta         </code>  </li>
     *     <li><code> lime_terracotta           </code>  </li>
     *     <li><code> pink_terracotta           </code>  </li>
     *     <li><code> gray_terracotta           </code>  </li>
     *     <li><code> light_gray_terracotta     </code>  </li>
     *     <li><code> cyan_terracotta           </code>  </li>
     *     <li><code> purple_terracotta         </code>  </li>
     *     <li><code> blue_terracotta           </code>  </li>
     *     <li><code> brown_terracotta          </code>  </li>
     *     <li><code> green_terracotta          </code>  </li>
     *     <li><code> red_terracotta            </code>  </li>
     *     <li><code> black_terracotta          </code>  </li>
     * </ul>
     * </div>
     */

    public void API_BlockManipulation()
    {
        this.expr.addLazyFunction("block", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            Value retval = BlockValue.fromParams(cc, lv, 0, true).block;
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("block_data", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            CompoundTag tag = BlockValue.fromParams(cc, lv, 0, true).block.getData();
            if (tag == null)
                return (c_, t_) -> Value.NULL;
            Value retval = new NBTSerializableValue(tag);
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("pos", 1, (c, t, lv) ->
        {
            Value arg = lv.get(0).evalValue(c);
            if (arg instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) arg).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Cannot fetch position of an unrealized block");
                Value retval = ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
                return (c_, t_) -> retval;
            }
            else if (arg instanceof EntityValue)
            {
                Entity e = ((EntityValue) arg).getEntity();
                if (e == null)
                    throw new InternalExpressionException("Null entity");
                Value retval = ListValue.of(new NumericValue(e.x), new NumericValue(e.y), new NumericValue(e.z));
                return (c_, t_) -> retval;
            }
            else
            {
                throw new InternalExpressionException("pos works only with a block or an entity type");
            }
        });

        this.expr.addLazyFunction("solid", -1, (c, t, lv) ->
                genericStateTest(c, "solid", lv, (s, p, w) -> new NumericValue(s.isSimpleFullBlock(w, p))));

        this.expr.addLazyFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        this.expr.addLazyFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        this.expr.addLazyFunction("flammable", -1, (c, t, lv) ->
                booleanStateTest(c, "flammable", lv, (s, p) -> !s.getMaterial().isBurnable()));

        this.expr.addLazyFunction("transparent", -1, (c, t, lv) ->
                booleanStateTest(c, "transparent", lv, (s, p) -> !s.getMaterial().isSolid()));

        /*this.expr.addLazyFunction("opacity", -1, (c, t, lv) ->
                genericStateTest(c, "opacity", lv, (s, p, w) -> new NumericValue(s.getOpacity(w, p))));

        this.expr.addLazyFunction("blocks_daylight", -1, (c, t, lv) ->
                genericStateTest(c, "blocks_daylight", lv, (s, p, w) -> new NumericValue(s.propagatesSkylightDown(w, p))));*/ // investigate

        this.expr.addLazyFunction("emitted_light", -1, (c, t, lv) ->
                genericStateTest(c, "emitted_light", lv, (s, p, w) -> new NumericValue(s.getLuminance())));

        this.expr.addLazyFunction("light", -1, (c, t, lv) ->
                genericStateTest(c, "light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(p))));

        this.expr.addLazyFunction("block_light", -1, (c, t, lv) ->
                genericStateTest(c, "block_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.BLOCK, p))));

        this.expr.addLazyFunction("sky_light", -1, (c, t, lv) ->
                genericStateTest(c, "sky_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.SKY, p))));

        this.expr.addLazyFunction("see_sky", -1, (c, t, lv) ->
                genericStateTest(c, "see_sky", lv, (s, p, w) -> new NumericValue(w.isSkyVisible(p))));

        this.expr.addLazyFunction("hardness", -1, (c, t, lv) ->
                genericStateTest(c, "hardness", lv, (s, p, w) -> new NumericValue(s.getHardness(w, p))));

        this.expr.addLazyFunction("blast_resistance", -1, (c, t, lv) ->
                genericStateTest(c, "blast_resistance", lv, (s, p, w) -> new NumericValue(s.getBlock().getBlastResistance())));

        this.expr.addLazyFunction("top", -1, (c, t, lv) ->
        {
            String type = lv.get(0).evalValue(c).getString().toLowerCase(Locale.ROOT);
            Heightmap.Type htype;
            switch (type)
            {
                //case "light": htype = Heightmap.Type.LIGHT_BLOCKING; break;  //investigate
                case "motion": htype = Heightmap.Type.MOTION_BLOCKING; break;
                case "terrain": htype = Heightmap.Type.MOTION_BLOCKING_NO_LEAVES; break;
                case "ocean_floor": htype = Heightmap.Type.OCEAN_FLOOR; break;
                case "surface": htype = Heightmap.Type.WORLD_SURFACE; break;
                default: throw new InternalExpressionException("Unknown heightmap type: "+type);
            }
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext)c, lv, 1);
            BlockPos pos = locator.block.getPos();
            int x = pos.getX();
            int z = pos.getZ();
            Value retval = new NumericValue(((CarpetContext)c).s.getWorld().getChunk(x >> 4, z >> 4).sampleHeightmap(htype, x & 15, z & 15) + 1);
            return (c_, t_) -> retval;
            //BlockPos pos = new BlockPos(x,y,z);
            //return new BlockValue(source.getWorld().getBlockState(pos), pos);
        });

        this.expr.addLazyFunction("loaded", -1, (c, t, lv) ->
        {
            Value retval = ((CarpetContext) c).s.getWorld().isBlockLoaded(BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos()) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("loaded_ep", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockValue.fromParams((CarpetContext)c, lv, 0).block.getPos();
            Value retval = ((CarpetContext)c).s.getWorld().getChunkManager().shouldTickChunk(new ChunkPos(pos))?Value.TRUE : Value.FALSE;
            //Value retval = ((CarpetContext)c).s.getWorld().isAreaLoaded(pos.getX() - 32, 0, pos.getZ() - 32,
            //        pos.getX() + 32, 0, pos.getZ() + 32) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("suffocates", -1, (c, t, lv) ->
                genericStateTest(c, "suffocates", lv, (s, p, w) -> new NumericValue(s.canSuffocate(w, p))));

        this.expr.addLazyFunction("power", -1, (c, t, lv) ->
                genericStateTest(c, "power", lv, (s, p, w) -> new NumericValue(w.getReceivedRedstonePower(p))));

        this.expr.addLazyFunction("ticks_randomly", -1, (c, t, lv) ->
                booleanStateTest(c, "ticks_randomly", lv, (s, p) -> s.hasRandomTicks()));

        this.expr.addLazyFunction("update", -1, (c, t, lv) ->
                booleanStateTest(c, "update", lv, (s, p) ->
                {
                    ((CarpetContext) c).s.getWorld().updateNeighbor(p, s.getBlock(), p);
                    return true;
                }));

        this.expr.addLazyFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    World w = ((CarpetContext)c).s.getWorld();
                    s.onRandomTick(w, p, w.random);
                    return true;
                }));

        this.expr.addLazyFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    World w = ((CarpetContext)c).s.getWorld();
                    if (s.hasRandomTicks() || s.getFluidState().hasRandomTicks())
                        s.onRandomTick(w, p, w.random);
                    return true;
                }));

        this.expr.addLazyFunction("set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            if (lv.size() < 2 || lv.size() % 2 == 1)
                throw new InternalExpressionException("set block should have at least 2 params and odd attributes");
            BlockValue.LocatorResult targetLocator = BlockValue.fromParams(cc, lv, 0);
            BlockValue.LocatorResult sourceLocator = BlockValue.fromParams(cc, lv, targetLocator.offset, true);
            BlockState sourceBlockState = sourceLocator.block.getBlockState();
            BlockState targetBlockState = world.getBlockState(targetLocator.block.getPos());
            if (sourceLocator.offset < lv.size())
            {
                StateFactory<Block, BlockState> states = sourceBlockState.getBlock().getStateFactory();
                for (int i = sourceLocator.offset; i < lv.size(); i += 2)
                {
                    String paramString = lv.get(i).evalValue(c).getString();
                    Property<?> property = states.getProperty(paramString);
                    if (property == null)
                        throw new InternalExpressionException("property " + paramString + " doesn't apply to " + sourceLocator.block.getString());
                    String paramValue = lv.get(i + 1).evalValue(c).getString();
                    sourceBlockState = setProperty(property, paramString, paramValue, sourceBlockState);
                }
            }

            CompoundTag data = sourceLocator.block.getData();

            if (sourceBlockState == targetBlockState && data == null)
                return (c_, t_) -> Value.FALSE;
            CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;
            BlockPos targetPos = targetLocator.block.getPos();
            Clearable.clear(world.getBlockEntity(targetPos));
            world.setBlockState(targetPos, sourceBlockState, 2);

            if ( data != null)
            {
                BlockEntity be = world.getBlockEntity(targetPos);
                if (be != null)
                {
                    CompoundTag destTag = data.method_10553();
                    destTag.putInt("x", targetPos.getX());
                    destTag.putInt("y", targetPos.getY());
                    destTag.putInt("z", targetPos.getZ());
                    be.fromTag(destTag);
                    be.markDirty();
                }
            }

            CarpetSettings.impendingFillSkipUpdates = false;
            Value retval = new BlockValue(sourceBlockState, world, targetLocator.block.getPos());
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("destroy", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (state.isAir())
                return (c_, t_) -> Value.FALSE;
            BlockPos where = locator.block.getPos();
            BlockEntity be = world.getBlockEntity(where);
            long how = 0;
            if (lv.size() > locator.offset)
                how = NumericValue.asNumber(lv.get(locator.offset).evalValue(cc)).getLong();
            world.clearBlockState(where, false);
            world.playLevelEvent(null, 2001, where, Block.getRawIdFromState(state));
            if (how < 0)
            {
                Block.dropStack(world, where, new ItemStack(state.getBlock()));
            }
            else
            {
                ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE, 1);
                tool.addEnchantment(Enchantments.FORTUNE,(int)how);

                LootContext.Builder lootContext$Builder_1 = (new LootContext.Builder(world)).
                        setRandom(world.random).
                        put(LootContextParameters.POSITION, where).
                        put(LootContextParameters.BLOCK_STATE, state).
                        putNullable(LootContextParameters.BLOCK_ENTITY, be).
                        putNullable(LootContextParameters.THIS_ENTITY, cc.s.getEntity()).
                        put(LootContextParameters.TOOL, tool);
                state.getDroppedStacks(lootContext$Builder_1).forEach((stack) -> Block.dropStack(world, where, stack));

                //Block.dropStacks(state, world, where, be, cc.s.getEntity(), tool );
            }
            return (c_, t_) -> Value.TRUE;
        });

        this.expr.addLazyFunction("harvest", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("harvest takes at least 2 parameters: entity and block, or position, to harvest");
            CarpetContext cc = (CarpetContext)c;
            World world = cc.s.getWorld();
            Value entityValue = lv.get(0).evalValue(cc);
            if (!(entityValue instanceof EntityValue))
                return (c_, t_) -> Value.FALSE;
            Entity e = ((EntityValue) entityValue).getEntity();
            if (!(e instanceof ServerPlayerEntity))
                return (c_, t_) -> Value.FALSE;
            ServerPlayerEntity player = (ServerPlayerEntity)e;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 1);
            BlockPos where = locator.block.getPos();
            BlockState state = locator.block.getBlockState();
            Block block = state.getBlock();
            boolean success = false;
            if (!((block instanceof BedrockBlock || block instanceof BarrierBlock) && player.interactionManager.isSurvivalLike()))
                success = tryBreakBlock_copy_from_ServerPlayerInteractionManager(player, where);
            if (success)
                world.playLevelEvent(null, 2001, where, Block.getRawIdFromState(state));
            return success ? LazyValue.TRUE : LazyValue.FALSE;
        });

        this.expr.addLazyFunction("place_item", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("place_item takes at least 2 parameters: item and block, or position, to place onto");
            CarpetContext cc = (CarpetContext) c;
            String itemString = lv.get(0).evalValue(c).getString();
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 1);
            ItemStackArgument stackArg = NBTSerializableValue.parseItem(itemString);
            BlockPos where = new BlockPos(locator.vec);
            String facing = "up";
            if (lv.size() > locator.offset)
                facing = lv.get(locator.offset).evalValue(c).getString();
            boolean sneakPlace = false;
            if (lv.size() > locator.offset+1)
                sneakPlace = lv.get(locator.offset+1).evalValue(c).getBoolean();
            if (stackArg.getItem() instanceof BlockItem)
            {
                BlockItem blockItem = (BlockItem) stackArg.getItem();
                BlockValue.PlacementContext ctx;
                try
                {
                    ctx = BlockValue.PlacementContext.from(cc.s.getWorld(), where, facing, sneakPlace, stackArg.createStack(1, false));
                }
                catch (CommandSyntaxException e)
                {
                    throw new InternalExpressionException(e.getMessage());
                }
                if (!ctx.canPlace())
                    return (_c, _t) -> Value.FALSE;
                BlockState placementState = blockItem.getBlock().getPlacementState(ctx);
                if (placementState != null)
                {
                    if (placementState.canPlaceAt(cc.s.getWorld(), where))
                    {
                        CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;
                        cc.s.getWorld().setBlockState(where, placementState, 2);
                        CarpetSettings.impendingFillSkipUpdates = false;
                        BlockSoundGroup blockSoundGroup = placementState.getSoundGroup();
                        cc.s.getWorld().playSound(null, where, blockSoundGroup.getPlaceSound(), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                        return (_c, _t) -> Value.TRUE;
                    }
                }
            }
            return (_c, _t) -> Value.FALSE;
        });

        this.expr.addLazyFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.canPlaceAtSide(((CarpetContext) c).s.getWorld(), p, BlockPlacementEnvironment.LAND)));

        this.expr.addLazyFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getSoundGroup())));

        this.expr.addLazyFunction("material", -1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        this.expr.addLazyFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getTopMaterialColor(((CarpetContext)c).s.getWorld(), p))));

        this.expr.addLazyFunction("property", -1, (c, t, lv) ->
        {
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("property requires to specify a property to query");
            String tag = lv.get(locator.offset).evalValue(c).getString();
            StateFactory<Block, BlockState> states = state.getBlock().getStateFactory();
            Property<?> property = states.getProperty(tag);
            if (property == null)
                return LazyValue.NULL;
            Value retval = new StringValue(state.get(property).toString().toLowerCase(Locale.ROOT));
            return (_c, _t) -> retval;
        });

        this.expr.addLazyFunction("block_properties", -1, (c, t, lv) ->
        {
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateFactory<Block, BlockState> states = state.getBlock().getStateFactory();
            Value res = ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName())).collect(Collectors.toList())
            );
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("set_biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
            if (lv.size() == locator.offset)
                throw new InternalExpressionException("set_biome needs a biome name as an argument");
            String biomeName = lv.get(locator.offset+0).evalValue(c).getString();
            Biome biome = Registry.BIOME.get(new Identifier(biomeName));
            if (biome == null)
                throw new InternalExpressionException("Unknown biome: "+biomeName);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Chunk chunk = world.getChunk(pos);
            chunk.getBiomeArray()[(pos.getX() & 15) | (pos.getZ() & 15) << 4] = biome;
            this.forceChunkUpdate(pos, world);
            return LazyValue.NULL;
        });
        // need get_biome

    }

    /**
     * <h1>Inventory and Items API</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Manipulating inventories of blocks and entities</h2>
     * <p>Most functions in this category require inventory as the first argument. Inventory could be specified by
     * an entity, or a block, or position (three coordinates) of a potential block with inventory.
     * If the entity or a block doesn't
     * have an inventory, they typically do nothing and return null.</p>
     * <p>Most items returned are in the form of
     * a triple of item name, count, and nbt or the extra data associated with an item.
     * Manipulating of the nbt data can be costly, but retrieving them from the tuple to match other aspects is cheap</p>
     * <h3><code>stack_limit(item)</code></h3>
     * <p>Returns number indicating what is the stack limit for the item. Its typically 1 (non-stackable),
     * 16 (like buckets), or 64 - rest. It is recommended to consult this, as other inventory API functions
     * ignore normal stack limits, and it is up to the programmer to keep it at bay. As of 1.13, game checks for negative
     * numbers and setting an item to negative is the same as empty.</p>
     * <pre>
     *     stack_limit('wooden_axe') =&gt; 1
     *     stack_limit('ender_pearl') =&gt; 16
     *     stack_limit('stone') =&gt; 64
     * </pre>
     * <h3><code>inventory_size(inventory)</code></h3>
     * <p>Returns the size of the inventory for the entity or block in question. Returns null if the block or entity
     * don't have an inventory</p>
     * <pre>
     *     inventory_size(player()) =&gt; 41
     *     inventory_size(x,y,z) =&gt; 27 // chest
     *     inventory_size(block(pos)) =&gt; 5 // hopper
     * </pre>
     * <h3><code>inventory_get(inventory, slot)</code></h3>
     * <p>Returns the item in the corresponding inventory slot, or null if slot empty or inventory is invalid.
     * You can use negative numbers to indicate slots counted from 'the back'.</p>
     * <pre>
     *     inventory_get(player(), 0) =&gt; null // nothing in first hotbar slot
     *     inventory_get(x,y,z, 5) =&gt; ['stone', 1, {}]
     *     inventory_get(player(), -1) =&gt; ['diamond_pickaxe', 1, {Damage:4}] // slightly damaged diamond pick in the offhand
     * </pre>
     *
     * <h3><code>inventory_set(inventory, slot, count, item?, nbt?) </code></h3>
     * <p>Modifies or sets a stack in inventory. specify count 0 to empty the slot.
     * If item is not specified, keeps existing item, just modifies the count. If item is provided - replaces current item.
     * If nbt is provided - adds a tag to the stack at slot. Returns previous stack in that slot.</p>
     * <pre>
     *     inventory_set(player(), 0, 0) =&gt; ['stone', 64, {}] // player had a stack of stone in first hotbar slot
     *     inventory_set(player(), 0, 6) =&gt; ['diamond', 64, {}] // changed stack of diamonds in player slot to 6
     *     inventory_set(player(), 0, 1, 'diamond_axe','{Damage:5}') =&gt; null //added slightly damaged pick to first player slot
     * </pre>
     *
     * <h3><code>inventory_find(inventory, item, start_slot?, ), inventory_find(inventory, null, start_slot?) </code></h3>
     * <p>Finds the first slot with a corresponding item in the inventory, or if queried with null: the first empty slot.
     * Returns slot number if found, or null otherwise. Optional start_slot argument allows to skip all preceeding slots
     * allowing for efficient (so not slot-by-slot) inventory search for items.</p>
     * <pre>
     *     inventory_find(player(), 'stone') =&gt; 0 // player has stone in first hotbar slot
     *     inventory_find(player(), null) =&gt; null // player's inventory has no empty spot
     *     while( (slot = inventory_find(p, 'diamond', slot)) != null, 41, drop_item(p, slot) )
     *         // spits all diamonds from player inventory wherever they are
     *     inventory_drop(x,y,z, 0) =&gt; 64 // removed and spawned in the world a full stack of items
     * </pre>
     *
     * <h3><code>inventory_remove(inventory, item, amount?) </code></h3>
     * <p>Removes amount (defaults to 1) of item from inventory. If the inventory doesn't have the defined amount, nothing
     * happens, otherwise the given amount of items is removed wherever they are in the inventory. Returns boolean whether
     * the removal operation was successful. Easiest way to remove a specific item from player inventory without specifying
     * the slot.</p>
     * <pre>
     *     inventory_remove(player(), 'diamond') =&gt; 1 // removed diamond from player inventory
     *     inventory_remove(player(), 'diamond', 100) =&gt; 0 // player doesn't have 100 diamonds, nothing happened
     * </pre>
     *
     * <h3><code>drop_item(inventory, slot, amount?, )</code></h3>
     * <p>Drops the items from indicated inventory slot, like player that Q's an item or villager, that exchanges food.
     * You can Q items from block inventories as well. default amount is 0 - which is all from the slot. NOTE: hoppers are quick
     * enough to pick all the queued items from their inventory anyways.
     * Returns size of the actual dropped items.</p>
     * <pre>
     *     inventory_drop(player(), 0, 1) =&gt; 1 // Q's one item on the ground
     *     inventory_drop(x,y,z, 0) =&gt; 64 // removed and spawned in the world a full stack of items
     * </pre>
     * </div>
     */
    public void API_InventoryManipulation()
    {
        this.expr.addLazyFunction("stack_limit", 1, (c, t, lv) ->
        {
            ItemStackArgument item = NBTSerializableValue.parseItem(lv.get(0).evalValue(c).getString());
            Value res = new NumericValue(item.getItem().getMaxCount());
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("inventory_size", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            Value res = new NumericValue(inventoryLocator.inventory.getInvSize());
            return (_c, _t) -> res;
        });

        //inventory_get(<b, e>, <n>) -> item_triple
        this.expr.addLazyFunction("inventory_get", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() == inventoryLocator.offset)
            {
                List<Value> fullInventory = new ArrayList<>();
                for (int i = 0, maxi = inventoryLocator.inventory.getInvSize(); i < maxi; i++)
                {
                    fullInventory.add(ListValue.fromItemStack(inventoryLocator.inventory.getInvStack(i)));
                }
                Value res = ListValue.wrap(fullInventory);
                return (_c, _t) -> res;
            }
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.getInvSize())
                return (_c, _t) -> Value.NULL;
            Value res = ListValue.fromItemStack(inventoryLocator.inventory.getInvStack(slot));
            return (_c, _t) -> res;
        });

        //inventory_set(<b,e>, <n>, <count>, <item>, <nbt>)
        this.expr.addLazyFunction("inventory_set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() < inventoryLocator.offset+2)
                throw new InternalExpressionException("inventory_set requires at least slot number and new stack size, and optional new item");
            int slot = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+0).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.getInvSize())
                return (_c, _t) -> Value.NULL;
            int count = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            if (count == 0)
            {
                // clear slot
                ItemStack removedStack = inventoryLocator.inventory.removeInvStack(slot);
                //Value res = ListValue.fromItemStack(removedStack); // that tuple will be read only but cheaper if noone cares
                return (_c, _t) -> ListValue.fromItemStack(removedStack);
            }
            if (lv.size() < inventoryLocator.offset+3)
            {
                ItemStack previousStack = inventoryLocator.inventory.getInvStack(slot);
                ItemStack newStack = previousStack.copy();
                newStack.setCount(count);
                inventoryLocator.inventory.setInvStack(slot, newStack);
                return (_c, _t) -> ListValue.fromItemStack(previousStack);
            }
            CompoundTag nbt = null; // skipping one argument
            if (lv.size() > inventoryLocator.offset+3)
            {
                Value nbtValue = lv.get(inventoryLocator.offset+3).evalValue(c);
                if (nbtValue instanceof NBTSerializableValue)
                    nbt = ((NBTSerializableValue)nbtValue).getCompoundTag();
                else
                    nbt = new NBTSerializableValue(nbtValue.getString()).getCompoundTag();
            }
            ItemStackArgument newitem = NBTSerializableValue.parseItem(
                    lv.get(inventoryLocator.offset+2).evalValue(c).getString(),
                    nbt
            );

            ItemStack previousStack = inventoryLocator.inventory.getInvStack(slot);
            try
            {
                inventoryLocator.inventory.setInvStack(slot, newitem.createStack(count, false));
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }
            return (_c, _t) -> ListValue.fromItemStack(previousStack);
        });

        //inventory_find(<b, e>, <item> or null (first empty slot), <start_from=0> ) -> <N> or null
        this.expr.addLazyFunction("inventory_find", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            ItemStackArgument itemArg = null;
            if (lv.size() > inventoryLocator.offset)
            {
                Value secondArg = lv.get(inventoryLocator.offset+0).evalValue(c);
                if (!(secondArg instanceof NullValue))
                    itemArg = NBTSerializableValue.parseItem(secondArg.getString());
            }
            int startIndex = 0;
            if (lv.size() > inventoryLocator.offset+1)
            {
                startIndex = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            }
            startIndex = NBTSerializableValue.validateSlot(startIndex, inventoryLocator.inventory);
            for (int i = startIndex, maxi = inventoryLocator.inventory.getInvSize(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory.getInvStack(i);
                if ( (itemArg == null && stack.isEmpty()) || (itemArg != null && itemArg.getItem().equals(stack.getItem())) )
                {
                    Value res = new NumericValue(i);
                    return (_c, _t) -> res;
                }
            }
            return (_c, _t) -> Value.NULL;
        });



        //inventory_remove(<b, e>, <item>, <amount=1>) -> bool

        this.expr.addLazyFunction("inventory_remove", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() <= inventoryLocator.offset)
                throw new InternalExpressionException("inventory_set requires at least an item to be removed");
            ItemStackArgument searchItem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset).evalValue(c).getString());
            int amount = 1;
            if (lv.size() > inventoryLocator.offset+1)
            {
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            }
            // not enough
            if (((amount == 1) && (!inventoryLocator.inventory.containsAnyInInv(Sets.newHashSet(searchItem.getItem()))))
                    || (inventoryLocator.inventory.countInInv(searchItem.getItem()) < amount))
            {
                return (_c, _t) -> Value.FALSE;
            }
            for (int i = 0, maxi = inventoryLocator.inventory.getInvSize(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory.getInvStack(i);
                if (stack.isEmpty())
                    continue;
                if (!stack.getItem().equals(searchItem.getItem()))
                    continue;
                int left = stack.getCount()-amount;
                if (left > 0)
                {
                    stack.setCount(left);
                    inventoryLocator.inventory.setInvStack(i, stack);
                    return (_c, _t) -> Value.TRUE;
                }
                else
                {
                    inventoryLocator.inventory.removeInvStack(i);
                    amount -= stack.getCount();
                }
            }
            if (amount > 0)
                throw new InternalExpressionException("Something bad happened - cannot pull all items from inventory");
            return (_c, _t) -> Value.TRUE;
        });

        //inventory_drop(<b, e>, <n>, <amount=1, 0-whatever's there>) -> entity_item (and sets slot) or null if cannot
        this.expr.addLazyFunction("drop_item", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() == inventoryLocator.offset)
                throw new InternalExpressionException("slot number is required for inventory_drop");
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.getInvSize())
                return (_c, _t) -> Value.NULL;
            int amount = 0;
            if (lv.size() > inventoryLocator.offset+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            if (amount < 0)
                throw new InternalExpressionException("Cannot throw negative number of items");
            ItemStack stack = inventoryLocator.inventory.getInvStack(slot);
            if (stack == null || stack.isEmpty())
                return (_c, _t) -> Value.ZERO;
            if (amount == 0)
                amount = stack.getCount();
            ItemStack droppedStack = inventoryLocator.inventory.takeInvStack(slot, amount);
            if (droppedStack.isEmpty())
            {
                return (_c, _t) -> Value.ZERO;
            }
            Object owner = inventoryLocator.owner;
            ItemEntity item;
            if (owner instanceof PlayerEntity)
            {
                item = ((PlayerEntity) owner).dropItem(droppedStack, false, true);
            }
            else if (owner instanceof LivingEntity)
            {
                LivingEntity villager = (LivingEntity)owner;
                // stolen from LookTargetUtil.give((VillagerEntity)owner, droppedStack, (LivingEntity) owner);
                double double_1 = villager.y - 0.30000001192092896D + (double)villager.getStandingEyeHeight();
                item = new ItemEntity(villager.world, villager.x, double_1, villager.z, droppedStack);
                Vec3d vec3d_1 = villager.getRotationVec(1.0F).normalize().multiply(0.3);//  new Vec3d(0, 0.3, 0);
                item.setVelocity(vec3d_1);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            else
            {
                Vec3d point = new Vec3d(inventoryLocator.position);
                item = new ItemEntity(cc.s.getWorld(), point.x+0.5, point.y+0.5, point.z+0.5, droppedStack);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            inventoryLocator.inventory.markDirty();
            Value res = new NumericValue(item.getStack().getCount());
            return (_c, _t) -> res;
        });
    }


    /**
     * <h1>Entity API</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Entity Selection</h2>
     * <p>Entities have to be fetched before using them. Entities can also change their state between calls to the script
     * if game happens either in between separate calls to the programs, or if the program calls <code>game_tick</code>
     * on its own. In this case - entities would need to be re-fetched, or the code should account for entities getting dead</p>
     * <h3><code>player(), player(type), player(name)</code></h3>
     * <p>
     * With no arguments, it returns the calling player or the player closest to the caller. Note that the main context
     * will receive <code>p</code> variable pointing to this player. With <code>type</code> or <code>name</code> specified
     * it will try first to match a type, returning a list of players matching a type, and if this fails, will assume its
     * player name query retuning player with that name, or <code>null</code> if no player was found.
     * With <code>'all'</code>, list of all players in the game, in all dimensions, so end user needs to be cautious, that
     * you might be referring to wrong blocks and entities around the player in question.
     * WIth <code>type = '*'</code> it returns all players in caller dimension, <code>'survival'</code> returns all survival
     * and adventure players, <code>'creative'</code> returns all creative players, <code>'spectating'</code> returns all spectating
     * players, and <code>'!spectating'</code>, all not-spectating players. If all fails,
     * with <code>name</code>, the player in question, if is logged in.</p>
     * <h3><code>entity_id(uuid), entity_id(id)</code></h3>
     * <p>Fetching entities wither by their ID obtained via <code>entity ~ 'id'</code>, which is unique
     * for a dimension and current world run, or by UUID, obtained via <code>entity ~ 'uuid'</code>.
     * It returns null if no such entity
     * is found. Safer way to 'store' entities between calls, as missing entities will be returning <code>null</code>.
     * Both calls using UUID or numerical ID are <code>O(1)</code>, but obviously using UUIDs takes more memory and compute.</p>
     * <h3><code>entity_list(type)</code></h3>
     * <p>Returns global lists of entities in the current dimension of a specified type. Currently the following selectors are available:</p>
     * <ul>
     *     <li><code>*</code>: all</li>
     *     <li><code>living</code></li>
     *     <li><code>items</code></li>
     *     <li><code>players</code></li>
     *     <li><code>!players</code></li>
     * </ul>
     *
     * <h3><code>entity_area(type, cx, cy, cz, dx, dy, dz)</code></h3>
     * <p>Returns entities of a specified type in an area centered on <code>cx, cy, cz</code> and
     * at most <code>dx, dy, dz</code> blocks away from the center point. Uses same selectors as <code>entities_list</code></p>
     *
     * <h3><code>entity_selector(selector)</code></h3>
     * <p>Returns entities satisfying given vanilla entity selector. Most complex among all the methods of selecting
     * entities, but the most capable. Selectors are cached so should be as fast as other methods of selecting entities.</p>
     *
     * <h3><code>spawn(name, pos, nbt?)</code></h3>
     * <p>Spawns and places an entity in world, like <code>/summon</code> vanilla command.
     * Requires a position to spawn, and optional extra nbt data to merge with the entity. What makes it different from calling
     * <code>run('summon ...')</code>, is the fact that you get the entity back as a return value, which is swell.</p>
     *
     * <h2>Entity Manipulation</h2>
     *
     * <p>Unlike with blocks, that use plethora of vastly different querying functions, entities are queried with
     * <code>query</code> function and altered via <code>modify</code> function. Type of information needed or
     * values to be modified are different for each call</p>
     * <p>Using <code>~</code> (in) operator is an alias for <code>query</code>. Especially useful if a statement has
     * no arguments, which in this case can be radically simplified</p>
     * <pre>
     *     query(p, 'name') &lt;=&gt; p ~ 'name'     // much shorter and cleaner
     *     query(p, 'holds', 'offhand') &lt;=&gt; p ~ l('holds', 'offhand')    // not really but can be done
     * </pre>
     * <h3><code>query(e,'removed')</code></h3>
     * <p>Boolean. True if the entity is removed</p>
     * <h3><code>query(e,'id')</code></h3>
     * <p>Returns numerical id of the entity. Most efficient way to keep track of entites in a script. Ids are only unique
     * within current game session (ids are not preserved between restarts), and dimension (each dimension has its own ids
     * which can overlap. </p>
     * <h3><code>query(e,'uuid')</code></h3>
     * <p>Returns UUID (unique id) of the entity. Can be used to access entities with the other vanilla commands and remains unique
     * regardless of the dimension, and is preserved between game restarts.
     * Apparently players cannot be accessed via UUID, but name instead.</p>
     * <pre>
     * map(entities_area('*',x,y,z,30,30,30),run('kill '+query(_,'id'))) // doesn't kill the player
     * </pre>
     * <h3><code>query(e,'pos')</code></h3>
     * <p>Triple of entity position</p>
     * <h3><code>query(e,'location')</code></h3>
     * <p>Quin-tuple of entity position (x, y, and z coords), and rotation (yaw, pitch)</p>
     * <h3><code>query(e,'x'), query(e,'y'), query(e,'z')</code></h3>
     * <p>Respective entity coordinate</p>
     * <h3><code>query(e,'pitch'), query(e,'yaw')</code></h3>
     * <p>Pitch and Yaw or where entity is looking.</p>
     * <h3><code>query(e,'look')</code></h3>
     * <p>Returns a 3d vector where the entity is looking.</p>
     * <h3><code>query(e,'motion')</code></h3>
     * <p>Triple of entity motion vector, <code>l(motion_x, motion_y, motion_z)</code></p>
     * <h3><code>query(e,'motion_x'), query(e,'motion_y'), query(e,'motion_z')</code></h3>
     * <p>Respective component of the motion vector</p>
     * <h3><code>query(e,'name'), query(e,'custom_name'), query(e,'type')</code></h3>
     * <p>String of entity name</p>
     * <pre>
     * query(e,'name')  =&gt; Leatherworker
     * query(e,'custom_name')  =&gt; null
     * query(e,'type')  =&gt; villager
     * </pre>
     * <h3><code>query(e,'is_riding')</code></h3>
     * <p>Boolean. True if riding another entity.</p>
     * <h3><code>query(e,'is_ridden')</code></h3>
     * <p>Boolean. True if another entity is riding it.</p>
     * <h3><code>query(e,'passengers')</code></h3>
     * <p>List of entities riding the entity.</p>
     * <h3><code>query(e,'mount')</code></h3>
     * <p>Entity that <code>e</code> rides.</p>
     * <h3><code>query(e,'tags')</code></h3>
     * <p>List of entity tags.</p>
     * <h3><code>query(e,'has_tags',tag)</code></h3>
     * <p>Boolean, True if the entity is marked with <code>tag</code>.</p>
     * <h3><code>query(e,'is_burning')</code></h3>
     * <p>Boolean, True if the entity is burning.</p>
     * <h3><code>query(e,'fire')</code></h3>
     * <p>Number of remaining ticks of being on fire.</p>
     * <h3><code>query(e,'silent')</code></h3>
     * <p>Boolean, True if the entity is silent.</p>
     * <h3><code>query(e,'gravity')</code></h3>
     * <p>Boolean, True if the entity is affected by gravity, like most entities do.</p>
     * <h3><code>query(e,'immune_to_fire')</code></h3>
     * <p>Boolean, True if the entity is immune to fire.</p>
     * <h3><code>query(e,'dimension')</code></h3>
     * <p>Name of the dimension entity is in.</p>
     * <h3><code>query(e,'height')</code></h3>
     * <p>Height of the entity.</p>
     * <h3><code>query(e,'width')</code></h3>
     * <p>Width of the entity.</p>
     * <h3><code>query(e,'eye_height')</code></h3>
     * <p>Eye height of the entity.</p>
     * <h3><code>query(e,'age')</code></h3>
     * <p>Age, in ticks, of the entity, i.e. how long it existed.</p>
     * <h3><code>query(e,'item')</code></h3>
     * <p>Name of the item if its an item entity, <code>null</code> otherwise</p>
     * <h3><code>query(e,'count')</code></h3>
     * <p>Number of items in a stack from item entity, <code>null</code> otherwise</p>
     * <h3><code>query(e,'is_baby')</code></h3>
     * <p>Boolean, true if its a baby.</p>
     * <h3><code>query(e,'target')</code></h3>
     * <p>Returns mob's attack target or null if none or not applicable.</p>
     * <h3><code>query(e,'home')</code></h3>
     * <p>Returns creature's home position or null if none or not applicable.</p>
     * <h3><code>query(e,'sneaking')</code></h3>
     * <p>Boolean, true if entity is sneaking.</p>
     * <h3><code>query(e,'sprinting')</code></h3>
     * <p>Boolean, true if entity is sprinting.</p>
     * <h3><code>query(e,'swimming')</code></h3>
     * <p>Boolean, true if entity is swimming.</p>
     * <h3><code>query(e,'jumping')</code></h3>
     * <p>Boolean, true if entity is jumping.</p>
     * <h3><code>query(e,'gamemode')</code></h3>
     * <p>String with gamemode, or <code>null</code> if not a player.</p>
     * <h3><code>query(e,'gamemode_id')</code></h3>
     * <p>Good'ol gamemode id, or null if not a player.</p>
     * <h3><code>query(e,'effect',name?)</code></h3>
     * <p>Without extra arguments, it returns list of effect active on a living entity.
     * Each entry is a triple of short effect name, amplifier, and remaining duration.
     * With an argument, if the living entity has not that potion active, returns <code>null</code>, otherwise
     * return a tuple of amplifier and remaining duration</p>
     * <pre>
     * query(p,'effect')  =&gt; [[haste, 0, 177], [speed, 0, 177]]
     * query(p,'effect','haste')  =&gt; [0, 177]
     * query(p,'effect','resistance')  =&gt; null
     * </pre>
     * <h3><code>query(e,'health')</code></h3>
     * <p>Number indicating remaining entity health, or <code>null</code> if not applicable.</p>
     *
     * <h3><code>query(e,'holds',slot?)</code></h3>
     * <p>Returns triple of short name, stack count, and NBT of item held in <code>slot</code>.
     * Available options for <code>slot</code> are:</p>
     * <ul>
     *     <li><code>mainhand</code></li>
     *     <li><code>offhand</code></li>
     *     <li><code>head</code></li>
     *     <li><code>chest</code></li>
     *     <li><code>legs</code></li>
     *     <li><code>feet</code></li>
     * </ul>
     * <p>If <code>slot</code> is not specified, it defaults to the main hand.</p>
     * <h3><code>query(e,'selected_slot')</code></h3>
     * <p>Number indicating the selected slot of entity inventory. Currently only applicable to players.</p>
     * <h3><code>query(e,'facing', order?)</code></h3>
     * <p>Returns where the entity is facing. optional order (number from 0 to 5, and negative), indicating
     * primary directions where entity is looking at. From most prominent (order 0) to opposite (order 5, or -1)</p>
     * <h3><code>query(e,'trace', reach?, options?...)</code></h3>
     * <p>Returns the result of ray tracing from entity perspective, indicating what it is looking at. Default reach is 4.5
     * blocks (5 for creative players), and by default it traces for blocks and entities, identical to player attack tracing action.
     * This can be customized with <code>options</code>, use 'blocks' to trace for blocks only, 'liquids' to include liquid blocks
     * as possible results, and 'entities' to trace entities as well.</p>
     * <p>Regardless of the options selected, the result could be <code>null</code> if nothing is in reach, entity, if look
     * targets an antity, and block value if block is in reach. Tracing always hits blocks. Even if an entity tracing is requested
     * and there is a block in the way - the block will be returned.</p>
     * <h3><code>query(e,'nbt',path?)</code></h3>
     * <p>Returns full NBT of the entity. If path is specified, it fetches only that portion of the NBT,
     * that corresponds to the path. For specification of <code>path</code> attribute, consult
     * vanilla <code>/data get entity</code> command.</p>
     * <p>Note that calls to <code>nbt</code> are considerably more expensive comparing to other
     * calls in Minecraft API, and should only be used when there is no other option. Also returned
     * NBT is just a string to any retrieval of information post can currently only be done with matching
     * operator <code>~</code>. With time we are hoping not to support the <code>'nbt'</code> call better,
     * but rather to fill the API, so that <code>'nbt'</code> calls are not needed</p>
     * <h2>Entity Modification</h2>
     * <p>Like with entity querying, entity modifications happen through one function. Most position and movements
     * modifications don't work currently on players as their position is controlled by clients.</p>
     * <p>Currently there is no ability to modify NBT directly, but you could always use <code>run('data modify entity</code></p>
     * <h3><code>modify(e,'remove')</code></h3>
     * <p>Removes (not kills) entity from the game.</p>
     * <h3><code>modify(e,'kill')</code></h3>
     * <p>Kills the entity.</p>
     * <h3><code>modify(e, 'pos', x, y, z), modify(e, 'pos', l(x,y,z) )</code></h3>
     * <p>Moves the entity to a specified coords.</p>
     * <h3><code>modify(e, 'location', x, y, z, yaw, pitch), modify(e, 'location', l(x, y, z, yaw, pitch) )</code></h3>
     * <p>Changes full location vector all at once.</p>
     * <h3><code>modify(e, 'x', x), modify(e, 'y', y), modify(e, 'z', z)</code></h3>
     * <p>Moves the entity in.... one direction.</p>
     * <h3><code>modify(e, 'pitch', pitch), modify(e, 'yaw', yaw)</code></h3>
     * <p>Changes entity's pitch or yaw.</p>
     * <h3><code>modify(e, 'move', x, y, z), modify(e, 'move', l(x,y,z) )</code></h3>
     * <p>Moves the entity by a vector from its current location.</p>
     * <h3><code>modify(e, 'motion', x, y, z), modify(e, 'motion', l(x,y,z) )</code></h3>
     * <p>Sets the motion vector (where and how much entity is moving).</p>
     * <h3><code>modify(e, 'motion_z', x), modify(e, 'motion_y', y), modify(e, 'motion_z', z)</code></h3>
     * <p>Sets the corresponding component of the motion vector.</p>
     * <h3><code>modify(e, 'accelerate', x, y, z), modify(e, 'accelerate', l(x, y, z) )</code></h3>
     * <p>Adds a vector to the motion vector. Most realistic way to apply a force to an entity.</p>
     * <h3><code>modify(e, 'custom_name'), modify(e, 'custom_name', name )</code></h3>
     * <p>Sets a custom name for an entity. Removes a custom name if the argument is <code>null</code></p>
     * <h3><code>modify(e, 'dismount')</code></h3>
     * <p>Dismounts riding entity.</p>
     * <h3><code>modify(e, 'mount', other)</code></h3>
     * <p>Mounts the entity to the <code>other</code>.</p>
     * <h3><code>modify(e, 'drop_passengers')</code></h3>
     * <p>Shakes off all passengers.</p>
     * <h3><code>modify(e, 'mount_passengers', passenger, ? ...), modify(e, 'mount_passengers', l(passengers) )</code></h3>
     * <p>Mounts on all listed entities on <code>e</code>.</p>
     * <h3><code>modify(e, 'tag', tag, ? ...), modify(e, 'tag', l(tags) )</code></h3>
     * <p>Adds tag / tags to the entity.</p>
     * <h3><code>modify(e, 'clear_tag', tag, ? ...), modify(e, 'clear_tag', l(tags) )</code></h3>
     * <p>Removes tag from entity.</p>
     * <h3><code>modify(e, 'talk')</code></h3>
     * <p>Make noises.</p>
     * <h3><code>modify(e, 'home', null), modify(e, 'home', block, distance?), modify(e, 'home', x, y, z, distance?)</code></h3>
     * <p>Sets AI to stay around the home position, within <code>distance</code> blocks from it. <code>distance</code>
     * defaults to 16 blocks. <code>null</code> removes it. <i>May</i> not work fully with mobs that have this AI built in, like
     * Villagers.</p>
     * </div>
     */

    public void API_EntityManipulation()
    {
        this.expr.addLazyFunction("player", -1, (c, t, lv) -> {
            if (lv.size() ==0)
            {

                Entity callingEntity = ((CarpetContext)c).s.getEntity();
                if (callingEntity instanceof PlayerEntity)
                {
                    Value retval = new EntityValue(callingEntity);
                    return (_c, _t) -> retval;
                }
                Vec3d pos = ((CarpetContext)c).s.getPosition();
                PlayerEntity closestPlayer = ((CarpetContext)c).s.getWorld().getClosestPlayer(pos.x, pos.y, pos.z, -1.0, EntityPredicates.VALID_ENTITY);
                if (closestPlayer != null)
                {
                    Value retval = new EntityValue(closestPlayer);
                    return (_c, _t) -> retval;
                }
                return (_c, _t) -> Value.NULL;
            }
            String playerName = lv.get(0).evalValue(c).getString();
            Value retval = Value.NULL;
            if ("all".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getMinecraftServer().getPlayerManager().getPlayerList().
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("*".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers().
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("survival".equalsIgnoreCase(playerName))
            {
                retval =  ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers((p) -> p.interactionManager.isSurvivalLike()).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("creative".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers(PlayerEntity::isCreative).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("spectating".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers(PlayerEntity::isSpectator).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("!spectating".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers((p) -> !p.isSpectator()).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else
            {
                ServerPlayerEntity player = ((CarpetContext) c).s.getMinecraftServer().getPlayerManager().getPlayer(playerName);
                if (player != null)
                    retval = new EntityValue(player);
            }
            Value finalVar = retval;
            return (cc, tt) -> finalVar;
        });

        this.expr.addLazyFunction("spawn", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("spawn function takes mob name, and position to spawn");
            String entityString = lv.get(0).evalValue(c).getString();
            Identifier entityId;
            try
            {
                entityId = Identifier.fromCommandInput(new StringReader(entityString));
                EntityType type = Registry.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
                if (type == null || !type.isSummonable())
                    return LazyValue.NULL;
            }
            catch (CommandSyntaxException exception)
            {
                 return LazyValue.NULL;
            }

            BlockValue.VectorLocator position = BlockValue.locateVec(cc, lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            CompoundTag tag = new CompoundTag();
            boolean hasTag = false;
            if (lv.size() > position.offset)
            {
                hasTag = true;
                tag = new NBTSerializableValue(lv.get(position.offset).evalValue(c).getString()).getCompoundTag();
            }
            tag.putString("id", entityId.toString());
            Vec3d vec3d = position.vec;

            if (EntityType.getId(EntityType.LIGHTNING_BOLT).equals(entityId)) {
                LightningEntity lightningEntity_1 = new LightningEntity(cc.s.getWorld(), vec3d.x, vec3d.y, vec3d.z, false);
                cc.s.getWorld().addLightning(lightningEntity_1);
                return LazyValue.NULL;
            } else {
                ServerWorld serverWorld = cc.s.getWorld();
                Entity entity_1 = EntityType.loadEntityWithPassengers(tag, serverWorld, (entity_1x) -> {
                    entity_1x.setPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, entity_1x.yaw, entity_1x.pitch);
                    return !serverWorld.method_18768(entity_1x) ? null : entity_1x;
                });
                if (entity_1 == null) {
                    return LazyValue.NULL;
                } else {
                    if (!hasTag && entity_1 instanceof MobEntity) {
                        ((MobEntity)entity_1).initialize(serverWorld, serverWorld.getLocalDifficulty(new BlockPos(entity_1)), SpawnType.COMMAND, null, null);
                    }
                    Value res = new EntityValue(entity_1);
                    return (_c, _t) -> res;
                }
            }
        });

        this.expr.addLazyFunction("entity_id", 1, (c, t, lv) ->
        {
            Value who = lv.get(0).evalValue(c);
            Entity e;
            if (who instanceof NumericValue)
            {
                e = ((CarpetContext)c).s.getWorld().getEntityById((int)((NumericValue) who).getLong());
            }
            else
            {
                e = ((CarpetContext)c).s.getWorld().getEntity(UUID.fromString(who.getString()));
            }
            if (e==null)
            {
                return LazyValue.NULL;
            }
            return (cc, tt) -> new EntityValue(e);
        });

        this.expr.addLazyFunction("entity_list", 1, (c, t, lv) -> {
            String who = lv.get(0).evalValue(c).getString();
            Pair<EntityType<?>, Predicate<? super Entity>> pair = EntityValue.getPredicate(who);
            if (pair == null)
            {
                throw new InternalExpressionException("Unknown entity selection criterion: "+who);
            }
            List<Entity> entityList = ((CarpetContext)c).s.getWorld().getEntities(pair.getKey(), pair.getValue());
            Value retval = ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
            return (_c, _t ) -> retval;
        });

        this.expr.addLazyFunction("entity_area", 7, (c, t, lv) -> {
            BlockPos center = new BlockPos(
                    NumericValue.asNumber(lv.get(1).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(2).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(3).evalValue(c)).getDouble()
            );
            Box area = new Box(center).expand(
                    NumericValue.asNumber(lv.get(4).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(5).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(6).evalValue(c)).getDouble()
            );
            String who = lv.get(0).evalValue(c).getString();
            Pair<EntityType<?>, Predicate<? super Entity>> pair = EntityValue.getPredicate(who);
            if (pair == null)
            {
                throw new InternalExpressionException("Unknown entity selection criterion: "+who);
            }
            List<Entity> entityList = ((CarpetContext)c).s.getWorld().getEntities(pair.getKey(), area, pair.getValue());
            Value retval = ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
            return (_c, _t ) -> retval;
        });

        this.expr.addLazyFunction("entity_selector", -1, (c, t, lv) ->
        {
            String selector = lv.get(0).evalValue(c).getString();
            List<Value> retlist = new ArrayList<>();
            for (Entity e: EntityValue.getEntitiesFromSelector(((CarpetContext)c).s, selector))
            {
                retlist.add(new EntityValue(e));
            }
            return (c_, t_) -> ListValue.wrap(retlist);
        });

        this.expr.addLazyFunction("query", -1, (c, t, lv) -> {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("query_entity takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to query_entity should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            Value retval;
            if (lv.size()==2)
                retval = ((EntityValue) v).get(what, null);
            else if (lv.size()==3)
                retval = ((EntityValue) v).get(what, lv.get(2).evalValue(c));
            else
                retval = ((EntityValue) v).get(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return (cc, tt) -> retval;
        });

        // or update
        this.expr.addLazyFunction("modify", -1, (c, t, lv) -> {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("modify_entity takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to get should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            if (lv.size()==2)
                ((EntityValue) v).set(what, null);
            else if (lv.size()==3)
                ((EntityValue) v).set(what, lv.get(2).evalValue(c));
            else
                ((EntityValue) v).set(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return lv.get(0);
        });

        // or update
        this.expr.addLazyFunction("entity_event", -1, (c, t, lv) ->
        {
            if (lv.size()<3)
                throw new InternalExpressionException("entity_event requires at least 3 arguments, entity, event to be handled, and function name, with optional arguments");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to get should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            Value functionValue = lv.get(2).evalValue(c);
            String function = null;
            if (!(functionValue instanceof NullValue))
                function = functionValue.getString();
            List<Value> args = null;
            if (lv.size()==4)
                args = Collections.singletonList(lv.get(3).evalValue(c));
            else if (lv.size()>4)
            {
                args = new ArrayList<>(lv.subList(3, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList()));
            }

            ((EntityValue) v).setEvent((CarpetContext)c, what, function, args);

            return LazyValue.NULL;
        });
    }

    /**
     * <h1>Iterating over larger areas of blocks</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>These functions help scan larger areas of blocks without using generic loop functions,
     * like nested <code>loop</code>.</p>
     * <h2> </h2>
     * <h3><code>scan(cx, cy, cz, dx, dy, dz, px?, py?, pz?, expr)</code></h3>
     * <p>Evaluates expression over area of blocks defined by its center (<code>cx, cy, cz</code>),
     * expanded in all directions by <code>dx, dy, dz</code> blocks, or optionally in negative with <code>d</code> coords,
     * and <code>p</code> coords in positive values. <code>expr</code> receives <code>_x, _y, _z</code>
     * as coords of current analyzed block and <code>_</code> which represents the block itself.</p>
     * <h3><code>volume(x1, y1, z1, x2, y2, z2, expr)</code></h3>
     * <p>Evaluates expression for each block in the area, the same as the <code>scan</code>function, but using two opposite
     * corners of the rectangular cuboid. Any corners can be specified, its like you would do with <code>/fill</code> command</p>
     * <h3><code>neighbours(x, y, z), neighbours(block), neighbours(l(x,y,z))</code></h3>
     * <p>Returns the list of 6 neighbouring blocks to the argument. Commonly used with other loop functions like <code>for</code></p>
     * <pre>
     * for(neighbours(x,y,z),air(_)) =&gt; 4 // number of air blocks around a block
     * </pre>
     * <h3><code>rect(cx, cy, cz, dx?, dy?, dz?, px?, py?, pz?)</code></h3>
     * <p>returns an iterator, just like <code>range</code> function that iterates over rectangular cubarea of blocks. If
     * only center point is specified, it iterates over 27 blocks. If <code>d</code> arguments are specified, expands selection
     * of respective number of blocks in each direction. If <code>p</code> arguments are specified, it uses <code>d</code> for
     * negative offset, and <code>p</code> for positive.</p>
     * <h3><code>diamond(cx, cy, cz, radius?, height?)</code></h3>
     * <p>Iterates over a diamond like area of blocks. With no radius and height, its 7 blocks centered around the middle
     * (block + neighbours). With a radius specified, it expands shape on x and z coords, and with a custom height, on y. Any of these can be
     * zero as well. radius of 0 makes a stick, height of 0 makes a diamond shape pad.</p>
     * </div>
     */

    public void API_IteratingOverAreasOfBlocks()
    {
        this.expr.addLazyFunction("scan", -1, (c, t, lv) ->
        {
            int lvsise = lv.size();
            if (lvsise != 7 && lvsise != 10)
                throw new InternalExpressionException("scan takes 2, or 3 triples of coords, and the expression");
            int cx = (int) NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            int cy = (int) NumericValue.asNumber(lv.get(1).evalValue(c)).getLong();
            int cz = (int) NumericValue.asNumber(lv.get(2).evalValue(c)).getLong();
            int xrange = (int) NumericValue.asNumber(lv.get(3).evalValue(c)).getLong();
            int yrange = (int) NumericValue.asNumber(lv.get(4).evalValue(c)).getLong();
            int zrange = (int) NumericValue.asNumber(lv.get(5).evalValue(c)).getLong();
            int xprange = xrange;
            int yprange = yrange;
            int zprange = zrange;
            LazyValue expr;
            if (lvsise == 7)
            {
                expr = lv.get(6);
            }
            else
            {
                xprange = (int) NumericValue.asNumber(lv.get(6).evalValue(c)).getLong();
                yprange = (int) NumericValue.asNumber(lv.get(7).evalValue(c)).getLong();
                zprange = (int) NumericValue.asNumber(lv.get(8).evalValue(c)).getLong();
                expr = lv.get(9);
            }

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            for (int y=cy-yrange; y <= cy+yprange; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x=cx-xrange; x <= cx+xprange; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z=cz-zrange; z <= cz+zprange; z++)
                    {
                        int zFinal = z;

                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        Value blockValue = BlockValue.fromCoords(((CarpetContext)c), xFinal,yFinal,zFinal).bindTo("_");
                        c.setVariable( "_", (cc_, t_c) -> blockValue);
                        if (expr.evalValue(c, Context.BOOLEAN).getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        this.expr.addLazyFunction("volume", 7, (c, t, lv) ->
        {
            int xi = (int) NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            int yi = (int) NumericValue.asNumber(lv.get(1).evalValue(c)).getLong();
            int zi = (int) NumericValue.asNumber(lv.get(2).evalValue(c)).getLong();
            int xj = (int) NumericValue.asNumber(lv.get(3).evalValue(c)).getLong();
            int yj = (int) NumericValue.asNumber(lv.get(4).evalValue(c)).getLong();
            int zj = (int) NumericValue.asNumber(lv.get(5).evalValue(c)).getLong();
            int minx = min(xi, xj);
            int miny = min(yi, yj);
            int minz = min(zi, zj);
            int maxx = max(xi, xj);
            int maxy = max(yi, yj);
            int maxz = max(zi, zj);
            LazyValue expr = lv.get(6);

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            for (int y=miny; y <= maxy; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x=minx; x <= maxx; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z=minz; z <= maxz; z++)
                    {
                        int zFinal = z;
                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        Value blockValue = BlockValue.fromCoords(((CarpetContext)c), xFinal,yFinal,zFinal).bindTo("_");
                        c.setVariable( "_", (cc_, t_c) -> blockValue);
                        if (expr.evalValue(c, Context.BOOLEAN).getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        this.expr.addLazyFunction("neighbours", -1, (c, t, lv)->
        {

            BlockPos center = BlockValue.fromParams((CarpetContext) c, lv,0).block.getPos();
            ServerWorld world = ((CarpetContext) c).s.getWorld();

            List<Value> neighbours = new ArrayList<>();
            neighbours.add(new BlockValue(null, world, center.up()));
            neighbours.add(new BlockValue(null, world, center.down()));
            neighbours.add(new BlockValue(null, world, center.north()));
            neighbours.add(new BlockValue(null, world, center.south()));
            neighbours.add(new BlockValue(null, world, center.east()));
            neighbours.add(new BlockValue(null, world, center.west()));
            Value retval = ListValue.wrap(neighbours);
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("rect", -1, (c, t, lv)->
        {
            if (lv.size() != 3 && lv.size() != 6 && lv.size() != 9)
            {
                throw new InternalExpressionException("rectangular region should be specified with 3, 6, or 9 coordinates");
            }
            int cx;
            int cy;
            int cz;
            int sminx;
            int sminy;
            int sminz;
            int smaxx;
            int smaxy;
            int smaxz;
            try
            {
                cx = (int)((NumericValue) lv.get(0).evalValue(c)).getLong();
                cy = (int)((NumericValue) lv.get(1).evalValue(c)).getLong();
                cz = (int)((NumericValue) lv.get(2).evalValue(c)).getLong();
                if (lv.size()==3) // only done this way because of stupid Java lambda final reqs
                {
                    sminx = 1;
                    sminy = 1;
                    sminz = 1;
                    smaxx = 1;
                    smaxy = 1;
                    smaxz = 1;
                }
                else if (lv.size()==6)
                {
                    sminx = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    sminy = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                    sminz = (int) ((NumericValue) lv.get(5).evalValue(c)).getLong();
                    smaxx = sminx;
                    smaxy = sminy;
                    smaxz = sminz;
                }
                else // size == 9
                {
                    sminx = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    sminy = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                    sminz = (int) ((NumericValue) lv.get(5).evalValue(c)).getLong();
                    smaxx = (int)((NumericValue) lv.get(6).evalValue(c)).getLong();
                    smaxy = (int)((NumericValue) lv.get(7).evalValue(c)).getLong();
                    smaxz = (int)((NumericValue) lv.get(8).evalValue(c)).getLong();
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to rect");
            }
            CarpetContext cc = (CarpetContext)c;
            return (c_, t_) -> new LazyListValue()
            {
                int minx = cx-sminx;
                int miny = cy-sminy;
                int minz = cz-sminz;
                int maxx = cx+smaxx;
                int maxy = cy+smaxy;
                int maxz = cz+smaxz;
                int x;
                int y;
                int z;
                {
                    reset();
                }
                @Override
                public boolean hasNext()
                {
                    return y <= maxy;
                }

                @Override
                public Value next()
                {
                    Value r = BlockValue.fromCoords(cc, x,y,z);
                    //possibly reroll context
                    x++;
                    if (x > maxx)
                    {
                        x = minx;
                        z++;
                        if (z > maxz)
                        {
                            z = minz;
                            y++;
                            // hasNext should fail if we went over
                        }
                    }

                    return r;
                }

                @Override
                public void fatality()
                {
                    // possibly return original x, y, z
                    super.fatality();
                }

                @Override
                public void reset()
                {
                    x = minx;
                    y = miny;
                    z = minz;
                }
            };
        });

        this.expr.addLazyFunction("diamond", -1, (c, t, lv)->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() != 3 && lv.size() != 4 && lv.size() != 5)
            {
                throw new InternalExpressionException("diamond region should be specified with 3 to 5 coordinates");
            }

            int cx;
            int cy;
            int cz;
            int width;
            int height;
            try
            {
                cx = (int)((NumericValue) lv.get(0).evalValue(c)).getLong();
                cy = (int)((NumericValue) lv.get(1).evalValue(c)).getLong();
                cz = (int)((NumericValue) lv.get(2).evalValue(c)).getLong();
                if (lv.size()==3)
                {
                    Value retval = new ListValue(Arrays.asList(
                            BlockValue.fromCoords(cc, cx, cy-1, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz),
                            BlockValue.fromCoords(cc, cx-1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz-1),
                            BlockValue.fromCoords(cc, cx+1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz+1),
                            BlockValue.fromCoords(cc, cx, cy+1, cz)
                    ));
                    return (_c, _t ) -> retval;
                }
                else if (lv.size()==4)
                {
                    width = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    height = 0;
                }
                else // size == 5
                {
                    width = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    height = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to diamond");
            }
            if (height == 0)
            {
                return (c_, t_) -> new LazyListValue()
                {
                    int curradius;
                    int curpos;
                    {
                        reset();
                    }
                    @Override
                    public boolean hasNext()
                    {
                        return curradius <= width;
                    }

                    @Override
                    public Value next()
                    {
                        if (curradius == 0)
                        {
                            curradius = 1;
                            return BlockValue.fromCoords(cc, cx, cy, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3
                        Value block = BlockValue.fromCoords(cc, cx+(curradius-abs(curpos-2*curradius)), cy, cz-curradius+abs( abs(curpos-curradius)%(4*curradius) -2*curradius ));
                        curpos++;
                        if (curpos>=curradius*4)
                        {
                            curradius++;
                            curpos = 0;
                        }
                        return block;

                    }

                    @Override
                    public void reset()
                    {
                        curradius = 0;
                        curpos = 0;
                    }
                };
            }
            else
            {
                return (c_, t_) -> new LazyListValue()
                {
                    int curradius;
                    int curpos;
                    int curheight;
                    {
                        reset();
                    }
                    @Override
                    public boolean hasNext()
                    {
                        return curheight <= height;
                    }

                    @Override
                    public Value next()
                    {
                        if (curheight == -height || curheight == height)
                        {
                            return BlockValue.fromCoords(cc, cx, cy+curheight++, cz);
                        }
                        if (curradius == 0)
                        {
                            curradius++;
                            return BlockValue.fromCoords(cc, cx, cy+curheight, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3

                        Value block = BlockValue.fromCoords(cc, cx+(curradius-abs(curpos-2*curradius)), cy+curheight, cz-curradius+abs( abs(curpos-curradius)%(4*curradius) -2*curradius ));
                        curpos++;
                        if (curpos>=curradius*4)
                        {
                            curradius++;
                            curpos = 0;
                            if (curradius>width -abs(width*curheight/height))
                            {
                                curheight++;
                                curradius = 0;
                                curpos = 0;
                            }
                        }
                        return block;
                    }

                    @Override
                    public void reset()
                    {
                        curradius = 0;
                        curpos = 0;
                        curheight = -height;
                    }
                };
            }
        });
    }

    //TODO sounds
    /**
     * <h1>Auxiliary aspects</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Collection of other methods that control smaller, yet still important aspects of the game</p>
     * <h2>Sounds</h2>
     * <h3><code>sound(name, pos, volume?, pitch?)</code></h3>
     * <p>Plays a specific sound <code>name</code>, at block or position <code>pos</code>, with optional
     * <code>volume</code> and modified <code>pitch</code>. <code>pos</code> can be either a block, triple of coords,
     * or a list of thee numbers. Uses the same options as a corresponding <code>playsound</code> command.</p>
     * <h2>Particles</h2>
     * <h3><code>particle(name, pos, count?. spread?, speed?, playername?)</code></h3>
     * <p>Renders a cloud of particles <code>name</code> centered around <code>pos</code> position, by default
     * <code>count</code> 10 of them, default <code>speed</code> of 0, and to all players nearby, but these
     * options can be changed via optional arguments. Follow vanilla <code>/particle</code> command on details on those
     * options. Valid particle names are for example
     * <code>'angry_villager', 'item diamond', 'block stone', 'dust 0.8 0.1 0.1 4'</code></p>
     * <h3><code>particle_line(name, pos, pos2, density?)</code></h3>
     * <p>Renders a line of particles from point <code>pos</code> to <code>pos2</code> with supplied density (defaults 1),
     * which indicates how far part you would want particles to appear, so <code>0.1</code> means one every 10cm.</p>
     * <h3><code>particle_rect(name, pos, pos2, density?)</code></h3>
     * <p>Renders a cuboid of particles between point <code>pos</code> to <code>pos2</code> with supplied density.</p>
     * <h2>Markers</h2>
     * <h3><code>create_marker(text, pos, rotation?, block?)</code></h3>
     * <p>Spawns a (permanent) marker entity with text or block at position. Returns that entity for further manipulations</p>
     * <h3><code>remove_all_markers()</code></h3>
     * <p>Removes all scarpet markers from the loaded portion of the world in case you didn't want to do the cleanup</p>
     * <h2>System function</h2>
     * <h3><code>nbt(expr)</code></h3>
     * <p>Treats the argument as a nbt serializable string and returns its nbt value.
     * In case nbt is not in a correct nbt compound tag format, the execution of a script will stop with an error,
     * whenever that tag is used.</p>
     * <h3><code>print(expr)</code></h3>
     * <p>Displays the result of the expression to the chat. Overrides default <code>scarpet</code> behaviour of
     * sending everyting to stderr.</p>
     * <h3><code>run(expr)</code></h3>
     * <p>Runs a command from the string result of the <code>expr</code> expression, and returns its success count</p>
     * <h3><code>save()</code></h3>
     * <p>Performs autosave, saves all chunks, player data, etc. Useful for programs where autosave is disabled
     * due to performance reasons and saves the world only on demand.</p>
     * <h3><code>tick_time()</code></h3>
     * <p>Returns game tick counter. Can be used to run certain operations every n-th ticks, or to count in-game time</p>
     * <h3><code>game_tick(mstime?)</code></h3>
     * <p>Causes game to run for one tick. By default runs it and returns control to the program, but can optionally
     * accept expected tick length, in milliseconds. You can't use it to permanently change the game speed, but
     * setting longer commands with custom tick speeds can be interrupted via <code>/script stop</code> command</p>
     * <pre>
     * loop(1000,tick())  // runs the game as fast as it can for 1000 ticks
     * loop(1000,tick(100)) // runs the game twice as slow for 1000 ticks
     * </pre>
     * <h3><code>current_dimension()</code></h3>
     * <p>Returns current dimension that scripts run in.</p>
     * <h3><code>in_dimension(smth, expr)</code></h3>
     * <p>Evaluates the expression <code>expr</code> with different dimension execution context. <code>smth</code> can
     * be an entity, world-localized block, so not <code>block('stone')</code>, or a string representing a dimension like:
     * <code>'nether'</code>, <code>'end'</code> or <code>'overworld'</code>.</p>
     * <h3><code>schedule(delay, function, args...)</code></h3>
     * <p>Schedules a user defined function to run with a specified <code>delay</code> ticks of delay.
     * Scheduled functions run at the end of the tick, and they will run in order they were scheduled.</p>
     * <h3><code>plop(pos, what)</code></h3>
     * <p>Plops a structure or a feature at a given <code>pos</code>, so block, triple position coordinates
     * or a list of coordinates. To <code>what</code> gets plopped and exactly where it often depends on the
     * feature or structure itself. For example, all structures are chunk aligned, and often span multiple chunks.
     * Repeated calls to plop a structure in the same chunk would result either in the same strucuture generated on
     * top of each other, or with different state, but same position. Most
     * structures generate at specific altitudes, which are hardcoded, or with certain blocks around them. API will cancel
     * all extra position / biome / random requirements for structure / feature placement, but some hardcoded limitations
     * may still cause some of strucutures/features not to place. Some features require special blocks to be present, like
     * coral -&gt; water or ice spikes -&gt; snow block, and for some features, like fossils, placement is all sorts of
     * messed up.</p>
     * <p>
     * All generated structures will retain their properties, like mob spawning, however in many cases the world / dimension
     * itself has certain rules to spawn mobs, like plopping a nether fortress in the overworld will not spawn nether mobs
     * because nether mobs can spawn only in the nether, but plopped in the nether - will behave like a valid nether
     * fortress.</p>
     * <p><code>plop</code>  will not use world random number generator to generate structures and features, but its own.
     * This has a benefit that they will generate properly randomly, not the same time every time</p>
     * <p>Structure list:</p>
     * <ul>
     *
     *     <li><code>monument</code>: Ocean Monument. Generates at fixed Y coordinate, surrounds itself with water.</li>
     *     <li><code>fortress</code>: Nether Fortress. Altitude varies, but its bounded by the code.</li>
     *     <li><code>mansion</code>: Woodland Mansion</li>
     *     <li><code>jungle_temple</code>: Jungle Temple</li>
     *     <li><code>desert_temple</code>: Desert Temple. Generates at fixed Y altitude.</li>
     *     <li><code>end_city</code>: End City with Shulkers</li>
     *     <li><code>igloo</code>: Igloo</li>
     *     <li><code>shipwreck</code>: Shipwreck, version1?</li>
     *     <li><code>shipwreck2</code>: Shipwreck, version2?</li>
     *     <li><code>witchhut</code></li>
     *     <li><code>oceanruin, oceanruin_small, oceanruin_tall</code>: Stone variants of ocean ruins.</li>
     *     <li><code>oceanruin_warm, oceanruin_warm_small, oceanruin_warm_tall</code>: Sandstone variants of ocean ruins.</li>
     *     <li><code>treasure</code>: A treasure chest. Yes, its a whole structure.</li>
     *     <li><code>mineshaft</code>: A mineshaft.</li>
     *     <li><code>mineshaft_mesa</code>: A Mesa (Badlands) version of a mineshaft.</li>
     *     <li><code>village</code>: Plains, oak village.</li>
     *     <li><code>village_desert</code>: Desert, sandstone village.</li>
     *     <li><code>village_savanna</code>: Savanna, acacia village.</li>
     *     <li><code>village_taiga</code>: Taiga, spruce village.</li>
     * </ul>
     * <p>Feature list:</p>
     * <ul>
     *     <li><code>oak</code></li>
     *     <li><code>oak_large</code>: oak with branches.</li>
     *     <li><code>birch</code></li>
     *     <li><code>birch_large</code>: tall variant of birch tree.</li>
     *     <li><code>shrub</code>: low bushes that grow in jungles.</li>
     *     <li><code>shrub_acacia</code>: low bush but configured with acacia</li>
     *     <li><code>shrub_snowy</code>: low bush with white blocks</li>
     *     <li><code>jungle</code>: a tree</li>
     *     <li><code>spruce_matchstick</code>: tall spruce with minimal leafage.</li>
     *     <li><code>dark_oak</code></li>
     *     <li><code>acacia</code></li>
     *     <li><code>spruce</code></li>
     *     <li><code>oak_swamp</code>: oak with more leaves and vines.</li>
     *     <li><code>jungle_large</code>: 2x2 jungle tree</li>
     *     <li><code>spruce_matchstick_large</code>: 2x2 spruce tree with minimal leafage</li>
     *     <li><code>spruce_large</code>: 2x2 spruce tree</li>
     *     <li><code>well</code>: desert well</li>
     *     <li><code>grass</code>: a few spots of tall grass</li>
     *     <li><code>grass_jungle</code>: little bushier grass feature</li>
     *     <li><code>fern</code>: a few random ferns</li>
     *     <li><code>cactus</code>: random cacti</li>
     *     <li><code>dead_bush</code>: a few random dead bushi</li>
     *     <li><code>fossils</code>: underground fossils, placement little wonky</li>
     *     <li><code>mushroom_brown</code>: large brown mushroom.</li>
     *     <li><code>mushroom_red</code>: large red mushroom.</li>
     *     <li><code>ice_spike</code>: ice spike. Require snow block below to place.</li>
     *     <li><code>glowstone</code>: glowstone cluster. Required netherrack above it.</li>
     *     <li><code>melon</code>: a patch of melons</li>
     *     <li><code>pumpkin</code>: a patch of pumpkins</li>
     *     <li><code>sugarcane</code></li>
     *     <li><code>lilypad</code></li>
     *     <li><code>dungeon</code>: Dungeon. These are hard to place, and fail often.</li>
     *     <li><code>iceberg</code>: Iceberg. Generate at sea level.</li>
     *     <li><code>iceberg_blue</code>: Blue ice iceberg.</li>
     *     <li><code>lake</code></li>
     *     <li><code>lava_lake</code></li>
     *     <li><code>end_island</code></li>
     *     <li><code>chorus</code>: Chorus plant. Require endstone to place.</li>
     *     <li><code>sea_grass</code>: a patch of sea grass. Require water.</li>
     *     <li><code>sea_grass_river</code>: a variant.</li>
     *     <li><code>kelp</code></li>
     *     <li><code>coral_tree, coral_mushroom, coral_claw</code>: various coral types, random color.</li>
     *     <li><code>coral</code>: random coral structure. Require water to spawn.</li>
     *     <li><code>sea_pickle</code></li>
     *     <li><code>boulder</code>: A rocky, mossy formation from a giant taiga biome. Doesn't update client properly,
     *     needs relogging.</li>
     * </ul>
     * </div>
     */

    public void API_AuxiliaryAspects()
    {
        this.expr.addLazyFunction("sound", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            Identifier soundName = new Identifier(lv.get(0).evalValue(c).getString());
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 1);
            if (Registry.SOUND_EVENT.get(soundName) == null)
                throw new InternalExpressionException("No such sound: "+soundName.getPath());
            float volume = 1.0F;
            float pitch = 1.0F;
            if (lv.size() > 0+locator.offset)
            {
                volume = (float) NumericValue.asNumber(lv.get(0+locator.offset).evalValue(c)).getDouble();
                if (lv.size() > 1+locator.offset)
                {
                    pitch = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                }
            }
            Vec3d vec = locator.vec;
            double d0 = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
            int count = 0;
            for (ServerPlayerEntity player : cc.s.getWorld().getPlayers( (p) -> p.squaredDistanceTo(vec) < d0))
            {
                count++;
                player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(soundName, SoundCategory.PLAYERS, vec, volume, pitch));
            }
            int totalPlayed = count;
            return (_c, _t) -> new NumericValue(totalPlayed);
        });

        this.expr.addLazyFunction("particle", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer ms = cc.s.getMinecraftServer();
            ServerWorld world = cc.s.getWorld();
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 1);
            String particleName = lv.get(0).evalValue(c).getString();
            int count = 10;
            double speed = 0;
            float spread = 0.5f;
            ServerPlayerEntity player = null;
            if (lv.size() > locator.offset)
            {
                count = (int) NumericValue.asNumber(lv.get(locator.offset).evalValue(c)).getLong();
                if (lv.size() > 1+locator.offset)
                {
                    spread = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        speed = NumericValue.asNumber(lv.get(2 + locator.offset).evalValue(c)).getDouble();
                        if (lv.size() > 3 + locator.offset) // should accept entity as well as long as it is player
                        {
                            player = ms.getPlayerManager().getPlayer(lv.get(3 + locator.offset).evalValue(c).getString());
                        }
                    }
                }
            }
            ParticleEffect particle = getParticleData(particleName);
            Vec3d vec = locator.vec;
            if (player == null)
            {
                for (PlayerEntity p : (world.getPlayers()))
                {
                    world.spawnParticles((ServerPlayerEntity)p, particle, true, vec.x, vec.y, vec.z, count,
                            spread, spread, spread, speed);
                }
            }
            else
            {
                world.spawnParticles(player,
                        particle, true, vec.x, vec.y, vec.z, count,
                        spread, spread, spread, speed);
            }

            return (c_, t_) -> Value.TRUE;
        });

        this.expr.addLazyFunction("particle_line", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = getParticleData(particleName);
            BlockValue.VectorLocator pos1 = BlockValue.locateVec(cc, lv, 1);
            BlockValue.VectorLocator pos2 = BlockValue.locateVec(cc, lv, pos1.offset);
            int offset = pos2.offset;
            double density = (lv.size() > offset)? NumericValue.asNumber(lv.get(offset).evalValue(c)).getDouble():1.0;
            if (density <= 0)
            {
                throw new InternalExpressionException("Particle density should be positive");
            }
            Value retval = new NumericValue(drawParticleLine(world, particle, pos1.vec, pos2.vec, density));
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("particle_rect", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = getParticleData(particleName);
            BlockValue.VectorLocator pos1 = BlockValue.locateVec(cc, lv, 1);
            BlockValue.VectorLocator pos2 = BlockValue.locateVec(cc, lv, pos1.offset);
            int offset = pos2.offset;
            double density = 1.0;
            if (lv.size() > offset)
            {
                density = NumericValue.asNumber(lv.get(offset).evalValue(c)).getDouble();
            }
            if (density <= 0)
            {
                throw new InternalExpressionException("Particle density should be positive");
            }
            Vec3d a = pos1.vec;
            Vec3d b = pos2.vec;
            double ax = min(a.x, b.x);
            double ay = min(a.y, b.y);
            double az = min(a.z, b.z);
            double bx = max(a.x, b.x);
            double by = max(a.y, b.y);
            double bz = max(a.z, b.z);
            int pc = 0;
            pc += drawParticleLine(world, particle, new Vec3d(ax, ay, az), new Vec3d(ax, by, az), density);
            pc += drawParticleLine(world, particle, new Vec3d(ax, by, az), new Vec3d(bx, by, az), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, by, az), new Vec3d(bx, ay, az), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, ay, az), new Vec3d(ax, ay, az), density);

            pc += drawParticleLine(world, particle, new Vec3d(ax, ay, bz), new Vec3d(ax, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(ax, by, bz), new Vec3d(bx, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, by, bz), new Vec3d(bx, ay, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, ay, bz), new Vec3d(ax, ay, bz), density);

            pc += drawParticleLine(world, particle, new Vec3d(ax, ay, az), new Vec3d(ax, ay, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(ax, by, az), new Vec3d(ax, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, by, az), new Vec3d(bx, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, ay, az), new Vec3d(bx, ay, bz), density);
            int particleCount = pc;
            return (c_, t_) -> new NumericValue(particleCount);
        });

        this.expr.addLazyFunction("create_marker", -1, (c, t, lv) ->{
            CarpetContext cc = (CarpetContext)c;
            BlockState targetBlock = null;
            BlockValue.VectorLocator pointLocator;
            String name;
            try
            {
                Value nameValue = lv.get(0).evalValue(c);
                name = nameValue instanceof NullValue ? "" : nameValue.getString();
                pointLocator = BlockValue.locateVec(cc, lv, 1, true);
                if (lv.size()>pointLocator.offset)
                    targetBlock = BlockValue.fromParams(cc, lv, pointLocator.offset, true).block.getBlockState();

            }
            catch (IndexOutOfBoundsException e)
            {
                throw new InternalExpressionException("create_marker requires a name and three coordinates, with optional direction, and optional block on its head");
            }

            ArmorStandEntity armorstand = new ArmorStandEntity(EntityType.ARMOR_STAND, cc.s.getWorld());
            armorstand.setPositionAndAngles(
                    pointLocator.vec.x,
                    pointLocator.vec.y - ((targetBlock==null)?(armorstand.getHeight()+0.41):(armorstand.getHeight()-0.3)),
                    pointLocator.vec.z,
                    (float)pointLocator.yaw,
                    (float) pointLocator.pitch
            );
            armorstand.addScoreboardTag("__scarpet_marker");
            if (targetBlock != null)
                armorstand.setEquippedStack(EquipmentSlot.HEAD, new ItemStack(targetBlock.getBlock().asItem()));
            if (!name.isEmpty())
            {
                armorstand.setCustomName(new LiteralText(name));
                armorstand.setCustomNameVisible(true);
            }
            armorstand.setHeadRotation(new EulerAngle((int)pointLocator.pitch,0,0));
            armorstand.setNoGravity(true);
            armorstand.setInvisible(true);
            armorstand.setInvulnerable(true);
            cc.s.getWorld().spawnEntity(armorstand);
            EntityValue result = new EntityValue(armorstand);
            return (_c, _t) -> result;
        });

        this.expr.addLazyFunction("remove_all_markers", 0, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            int total = 0;
            for (Entity e : cc.s.getWorld().getEntities(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains("__scarpet_marker")))
            {
                total ++;
                e.remove();
            }
            int finalTotal = total;
            return (_cc, _tt) -> new NumericValue(finalTotal);
        });

        this.expr.addLazyFunction("nbt", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c);
            if (v instanceof NBTSerializableValue)
                return (cc, tt) -> v;
            Value ret = new NBTSerializableValue(v.getString());
            return (cc, tt) -> ret;
        });

        //"overridden" native call that prints to stderr
        this.expr.addLazyFunction("print", 1, (c, t, lv) ->
        {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value res = lv.get(0).evalValue(c);
            if (s.getEntity() instanceof  PlayerEntity)
            {
                Messenger.m((PlayerEntity) s.getEntity(), "w "+ res.getString());
            }
            else
            {
                Messenger.m(s, "w "+ res.getString());
            }
            return (_c, _t) -> res; // pass through for variables
        });


        this.expr.addLazyFunction("run", 1, (c, t, lv) -> {
            BlockPos target = ((CarpetContext)c).origin;
            Vec3d posf = new Vec3d((double)target.getX()+0.5D,(double)target.getY(),(double)target.getZ()+0.5D);
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new NumericValue(s.getMinecraftServer().getCommandManager().execute(
                    s.withPosition(posf).withSilent(), lv.get(0).evalValue(c).getString()));
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("save", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            s.getMinecraftServer().getPlayerManager().saveAllPlayerData();
            s.getMinecraftServer().save(true,true,true);
            s.getWorld().getChunkManager().tick(() -> true);
            CarpetSettings.LOG.warn("Saved chunks");
            return (cc, tt) -> Value.TRUE;
        });

        this.expr.addLazyFunction("tick_time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getMinecraftServer().getTicks());
            return (cc, tt) -> time;
        });

        this.expr.addLazyFunction("game_tick", -1, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            ((MinecraftServerInterface)s.getMinecraftServer()).forceTick( () -> System.nanoTime()- CarpetServer.scriptServer.tickStart<50000000L);
            if (lv.size()>0)
            {
                long ms_total = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
                long end_expected = CarpetServer.scriptServer.tickStart+ms_total*1000000L;
                long wait = end_expected-System.nanoTime();
                if (wait > 0L)
                {
                    try
                    {
                        Thread.sleep(wait/1000000L);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            CarpetServer.scriptServer.tickStart = System.nanoTime(); // for the next tick
            Thread.yield();
            if(CarpetServer.scriptServer.stopAll)
                throw new Expression.ExitStatement(Value.NULL);
            return (cc, tt) -> Value.TRUE;
        });

        this.expr.addLazyFunction("current_dimension", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new StringValue(s.getWorld().dimension.getType().toString().replaceFirst("minecraft:",""));
            return (cc, tt) -> retval;
        });

        this.expr.addLazyFunction("in_dimension", 2, (c, t, lv) -> {
            ServerCommandSource outerSource = ((CarpetContext)c).s;
            Value dimensionValue = lv.get(0).evalValue(c);
            ServerCommandSource innerSource = outerSource;
            if (dimensionValue instanceof EntityValue)
            {
                innerSource = outerSource.withWorld((ServerWorld) ((EntityValue)dimensionValue).getEntity().getEntityWorld());
            }
            else if (dimensionValue instanceof BlockValue)
            {
                BlockValue bv = (BlockValue)dimensionValue;
                if (bv.getWorld() != null)
                {
                    innerSource = outerSource.withWorld(bv.getWorld());
                }
                else
                {
                    throw new InternalExpressionException("in_dimension accepts only world-localized block arguments");
                }
            }
            else
            {
                String dimString = dimensionValue.getString().toLowerCase(Locale.ROOT);
                switch (dimString)
                {
                    case "nether":
                    case "the_nether":
                        innerSource = outerSource.withWorld(outerSource.getMinecraftServer().getWorld(DimensionType.THE_NETHER));
                        break;
                    case "end":
                    case "the_end":
                        innerSource = outerSource.withWorld(outerSource.getMinecraftServer().getWorld(DimensionType.THE_END));
                        break;
                    case "overworld":
                    case "over_world":
                        innerSource = outerSource.withWorld(outerSource.getMinecraftServer().getWorld(DimensionType.OVERWORLD));
                        break;
                    default:
                        throw new InternalExpressionException("Incorrect dimension string: "+dimString);
                }
            }
            ((CarpetContext) c).s = innerSource;
            Value retval = lv.get(1).evalValue(c);
            ((CarpetContext) c).s = outerSource;
            return (cc, tt) -> retval;
        });

        this.expr.addLazyFunction("plop", 4, (c, t, lv) ->{
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext)c, lv, 0);
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("plop needs extra argument indicating what to plop");
            String what = lv.get(locator.offset).evalValue(c).getString();
            Boolean res = FeatureGenerator.spawn(what, ((CarpetContext)c).s.getWorld(), locator.block.getPos());
            if (res == null)
                return (c_, t_) -> Value.NULL;
            if (what.equalsIgnoreCase("boulder"))  // there might be more of those
                this.forceChunkUpdate(locator.block.getPos(), ((CarpetContext)c).s.getWorld());
            return (c_, t_) -> new NumericValue(res);
        });

        this.expr.addLazyFunction("schedule", -1, (c, t, lv) -> {
            if (lv.size()<2)
                throw new InternalExpressionException("schedule should have at least 2 arguments, delay and call name");
            Long delay = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            String funname = lv.get(1).evalValue(c).getString();
            CarpetContext cc = (CarpetContext)c;
            if (!cc.host.globalFunctions.containsKey(funname))
                throw new InternalExpressionException("function "+funname+" is not defined");
            List<LazyValue> args = new ArrayList<>();
            for (int i=2; i < lv.size(); i++)
            {
                Value arg = lv.get(i).evalValue(cc);
                args.add( (_c, _t) -> arg);
            }
            if (cc.host.globalFunctions.get(funname).getArguments().size() != args.size())
                throw new InternalExpressionException("function "+funname+" takes "+
                        cc.host.globalFunctions.get(funname).getArguments().size()+" arguments, "+args.size()+" provided.");
            CarpetServer.scriptServer.events.scheduleCall(cc, funname, args, delay);
            return (c_, t_) -> Value.TRUE;
        });
    }

    /**
     * <h1>.</h1>
     * @param expression expression
     * @param source source
     * @param origin origin
     */
    public CarpetExpression(String expression, ServerCommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);

        API_BlockManipulation();
        API_EntityManipulation();
        API_InventoryManipulation();
        API_IteratingOverAreasOfBlocks();
        API_AuxiliaryAspects();
    }

    /**
     * <h1><code>/script scan</code>, <code>/script fill</code> and <code>/script outline</code> commands</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>These commands can be used to evaluate an expression over an area of blocks. They all need to have specified
     * the origin of the analyzed area (which is used as referenced (0,0,0), and two corners of an area to analyzed. If
     * you would want that the script block coordinates refer to the actual world coordinates, use origin of <code>0 0 0</code>,
     * or if it doesn't matter, duplicating coordinates of one of the corners is the easiest way.</p>
     * <p>These commands, unlike raw <code>/script run </code> are limited by vanilla fill / clone command
     * limit of 32k blocks, which can be altered with carpet mod's own <code>/carpet fillLimit</code> command.</p>
     * <h2></h2>
     * <h3><code>/script scan origin&lt;x y z&gt;  corner&lt;x y z&gt; corner&lt;x y z&gt; expr</code></h3>
     * <p>Evaluates expression for each point in the area and returns number of successes (result was positive). Since
     * the command by itself doesn't affect the area, the effects would be in side effects.</p>
     * <h3><code>/script fill origin&lt;x y z&gt;  corner&lt;x y z&gt; corner&lt;x y z&gt; "expr" &lt;block&gt; (? replace &lt;replacement&gt;) </code></h3>
     * <p>Think of it as a regular fill command, that sets blocks based on whether a result of the command was successful.
     * Note that the expression is in quotes. Thankfully string constants in <code>scarpet</code> use single quotes. Can be used
     * to fill complex geometric shapes.</p>
     * <h3><code>/script outline origin&lt;x y z&gt;  corner&lt;x y z&gt; corner&lt;x y z&gt; "expr" &lt;block&gt; (? replace &lt;replacement&gt;) </code></h3>
     * <p>Similar to <code>fill</code> command it evaluates an expression for each block in the area, but in this case setting blocks
     * where condition was true and any of the neighbouring blocks were evaluated negatively. This allows to create surface areas,
     * like sphere for example, without resorting to various rounding modes and tricks.</p>
     * <p>Here is an example of seven ways to draw a sphere of radius of 32 blocks around 0 100 0: </p>
     * <pre>
     * /script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z &lt;  32*32" white_stained_glass replace air
     * /script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z &lt;= 32*32" white_stained_glass replace air
     * /script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z &lt;  32.5*32.5" white_stained_glass replace air
     * /script fill    0 100 0 -40 60 -40 40 140 40 "floor(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
     * /script fill    0 100 0 -40 60 -40 40 140 40 "round(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
     * /script fill    0 100 0 -40 60 -40 40 140 40 "ceil(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
     * /draw sphere 0 100 0 32 white_stained_glass replace air
     *
     * fluffy ball round(sqrt(x*x+y*y+z*z)-rand(abs(y)))==32
     *
     * </pre>
     * <p>The last method is the one that world edit is using (part of carpet mod). It turns out that the outline method with <code>32.5</code> radius,
     * fill method with <code>round</code> function and draw command are equivalent</p>
     * </div>
     * @param host .
     * @param x .
     * @param y .
     * @param z .
     * @return .
     */
    public boolean fillAndScanCommand(ScriptHost host, int x, int y, int z)
    {
        if (CarpetServer.scriptServer.stopAll)
            return false;
        try
        {
            Context context = new CarpetContext(host, source, origin).
                    with("x", (c, t) -> new NumericValue(x - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(y - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(z - origin.getZ()).bindTo("z")).
                    with("_", (c, t) -> new BlockValue(null, source.getWorld(), new BlockPos(x, y, z)).bindTo("_"));
            Entity e = source.getEntity();
            if (e==null)
            {
                Value nullPlayer = Value.NULL.reboundedTo("p");
                context.with("p", (cc, tt) -> nullPlayer );
            }
            else
            {
                Value playerValue = new EntityValue(e).bindTo("p");
                context.with("p", (cc, tt) -> playerValue);
            }
            return this.expr.eval(context).getBoolean();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage());
        }
        catch (ArithmeticException ae)
        {
            throw new CarpetExpressionException("math doesn't compute... "+ae.getMessage());
        }
    }

    /**
     * <h1><code>/script run</code> command</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Primary way to input commands. The command executes in the context, position, and dimension of the executing
     * player, commandblock, etc... The command receives 4 variables, <code>x</code>, <code>y</code>, <code>z</code>
     * and <code>p</code> indicating position and the executing entity of the command.
     * You will receive tab completion suggestions as you type your code suggesting functions and global variables.
     * It is advisable to use <code>/execute in ... at ... as ... run script run ...</code> or similar, to simulate running
     * commands in a different scope</p>
     * </div>
     * @param host .
     * @param pos .
     * @return .
     */
    public String scriptRunCommand(ScriptHost host, BlockPos pos)
    {
        if (CarpetServer.scriptServer.stopAll)
            return "SCRIPTING PAUSED";
        try
        {
            Context context = new CarpetContext(host, source, origin).
                    with("x", (c, t) -> new NumericValue(pos.getX() - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(pos.getY() - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(pos.getZ() - origin.getZ()).bindTo("z"));
            Entity e = source.getEntity();
            if (e==null)
            {
                Value nullPlayer = Value.NULL.reboundedTo("p");
                context.with("p", (cc, tt) -> nullPlayer );
            }
            else
            {
                Value playerValue = new EntityValue(e).bindTo("p");
                context.with("p", (cc, tt) -> playerValue);
            }
            return this.expr.eval(context).getString();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage());
        }
        catch (ArithmeticException ae)
        {
            throw new CarpetExpressionException("math doesn't compute... "+ae.getMessage());
        }
    }

    /**
     * <h1><code>/script invoke / invokepoint / invokearea</code>, <code>/script globals</code> commands</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p><code>invoke</code> family of commands provide convenient way to invoke stored procedures (i.e. functions
     * that has been defined previously by any running script. To view current stored procedure set,
     * run <code>/script globals</code>, to define a new stored procedure, just run a <code>/script run function(a,b) -&gt; ( ... )</code>
     * command with your procedure once, and to forget a procedure, use <code>undef</code> function:
     * <code>/script run undef('function')</code></p>
     * <h2></h2>
     * <h3><code>/script invoke &lt;fun&gt; &lt;args?&gt; ... </code></h3>
     * <p>Equivalent of running <code>/script run fun(args, ...)</code>, but you get the benefit of getting the tab completion of the
     * command name, and lower permission level required to run these (since player is not capable of running any custom code
     * in this case, only this that has been executed before by an operator). Arguments will be checked for validity, and
     * you can only pass simple values as arguments (strings, numbers, or <code>null</code> value). Use quotes to include
     * whitespaces in argument strings.</p>
     * <p>Command will check provided arguments with required arguments (count) and fail if not enough or too much arguments
     * are provided. Operators defining functions are advised to use descriptive arguments names, as these will be visible
     * for invokers and form the base of understanding what each argument does.</p>
     * <p><code>invoke</code> family of commands will tab complete any stored function that does not start with <code>'_'</code>,
     * it will still allow to run procedures starting with <code>'_'</code> but not suggest them, and ban execution of any
     * hidden stored procedures, so ones that start with <code>'__'</code>. In case operator needs to use subroutines
     * for convenience and don't want to expose them to the <code>invoke</code> callers, they can use this mechanic.</p>
     * <pre>
     * /script run example_function(const, phrase, price) -&gt; print(const+' '+phrase+' '+price)
     * /script invoke example_function pi costs 5
     * </pre>
     * <h3><code>/script invokepoint &lt;fun&gt; &lt;coords x y z&gt; &lt;args?&gt; ... </code></h3>
     * <p>It is equivalent to <code>invoke</code> except it assumes that the first three arguments are coordinates, and provides
     * coordinates tab completion, with <code>looking at... </code> mechanics for convenience. All other arguments are expected
     * at the end</p>
     * <h3><code>/script invokearea &lt;fun&gt; &lt;coords x y z&gt; &lt;coords x y z&gt; &lt;args?&gt; ... </code></h3>
     * <p>It is equivalent to <code>invoke</code> except it assumes that the first three arguments are one set of ccordinates,
     * followed by the second set of coordinates, providing tab completion, with <code>looking at... </code> mechanics for convenience,
     * followed by any other required arguments</p>
     * </div>
     */

    public void invokeGlobalFunctionCommand()
    {

    }

    /**
     * <h1><code>/script load / unload &lt;module&gt; (shared?)</code>, <code>/script in &lt;module&gt;</code> commands</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p><code>load / unload</code> commands allow for very conventient way of writing your code, providing it to the game
     * and distribute with your worlds without the need of use of commandblocks. Just place your scarpet code in the /scripts
     * folder of your world files and make sure it ends with <code>.sc</code> extension. The good thing about editing that
     * code is that you can no only use normal editing without the need of marking of newlines, but you can also use
     * comments in your code.</p>
     * <p> a comment is anything that starts with a double slash, and continues to the end of the line:</p>
     * <pre>
     * foo = 1;
     * //This is a comment
     * bar = 2;
     * // This never worked, so I commented it out
     * // baz = foo()
     * </pre>
     * <h2></h2>
     * <h3><code>/script load/unload &lt;module&gt; (?shared) </code></h3>
     * <p>Loading operation will load that script code from disk and execute it right away. You would probably use it to
     * load some stored procedures to be used for later. To reload the module, just type <code>/script load</code> again.
     * Reloading removes all the current global state (globals and functions) that were added later by the module. </p>
     * <p>Loading a module, if it contains a <code>__command()</code> method, will attempt to registed a command with that
     * module name, and register all public (no underscore) functions available in the module as subcommands. It will also
     * bind specific events to the event system (check Events section for details).</p>
     * <p>Unloading a module removes all of its state from the game, disables commands, and removes bounded events</p>
     * <p>Scripts can be loaded in shared and player mode. Default is player, so all globals and stored functions are
     * individual for each player, meaning scripts don't need to worry about making sure they store some intermittent data
     * for each player independently. In shared mode - all global values and stored functions are shared among all players.
     * To access specific player data with commandblocks, use <code>/execute as (player) run script in (module) run ... </code>
     * To access global/server state, you need to disown the command from any player, so use a commandblock, or any arbitrary
     * entity: <code>/execute as @e[type=bat,limit=1] run script in (module) globals</code> for instance.
     * </p>
     * <h3><code>/script in &lt;module&gt; ... </code></h3>
     * <p>Allows to run normal /script commands in a specific module, like <code>run, invoke,..., globals</code> etc...</p>
     * </div>
     */
    public void loadScriptsFromFilesCommand()
    {

    }

    /**
     * <h1>Scarpet events system</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Provides the ability to execute specific function whenever an event occurs. The functions to be registered
     * need to conform with the arguments to the event specification. When loading module functions, each function that
     * starts with <code>__on_...</code> and has the required arguments, will be bound automatically.
     * In case of player specific modules, all player action events will be directed to the appropriate player space, and
     * all tick events will be executed in the global context, so its not a good idea to mix these two, so use either of these,
     * or use commands to call tick events directly, or handle player specific data inside a module.</p>
     * <h2></h2>
     * <h3>Event list</h3>
     * <p>Here is a list of events that can be handled by scarpet. This list includes prefixes required by modules to
     * autoload them, but you can add any function to any event if it had required parameters:</p>
     * <pre>
     * __on_tick()         // can access blocks and entities in the overworld
     * __on_tick_nether()  // can access blocks and entities in the nether
     * __on_tick_ender()   // can access blocks and entities in the end
     * // player specific callbacks
     * __on_player_jumps(player)
     * __on_player_deploys_elytra(player)
     * __on_player_wakes_up(player)
     * __on_player_rides(player, forward, strafe, jumping, sneaking)
     * __on_player_uses_item(player, item_tuple, hand)
     * __on_player_clicks_block(player, block, face)
     * __on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)
     * __on_player_breaks_block(player, block)
     * __on_player_interacts_with_entity(player, entity, hand)
     * __on_player_attacks_entity(player, entity)
     * __on_player_starts_sneaking(player)
     * __on_player_stops_sneaking(player)
     * __on_player_starts_sprinting(player)
     * __on_player_stops_sprinting(player)
     * </pre>
     * <h3><code>/script event</code> command</h3>
     * <p>used to display current events and bounded functions. use <code>add_to</code> ro register new event, or <code>remove_from</code>
     * to unbind a specific function from an event.</p>
     * </div>
     */
    public void gameEventsSystem()
    {

    }
}
