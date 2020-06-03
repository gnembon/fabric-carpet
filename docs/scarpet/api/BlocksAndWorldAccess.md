# Blocks / World API

## Specifying blocks

### `block(x, y, z)`, `block(l(x,y,z))`, `block(state)`

Returns either a block from specified location, or block with a specific state (as used by `/setblock` command), 
so allowing for block properties, block entity data etc. Blocks otherwise can be referenced everywhere by its simple 
string name, but its only used in its default state.

<pre>
block('air')  => air
block('iron_trapdoor[half=top]')  => iron_trapdoor
block(0,0,0) == block('bedrock')  => 1
block('hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') => hopper
</pre>

Retrieving a block with `block` function has also a side-effect of evaluating its current state and data. 
So if you use it later it will reflect block state and data of the block that was when block was called, rather than
when it was used. Block values passed in various places like `scan` functions, etc, are not fully evaluated unless 
its properties are needed. This means that if the block at the location changes before its queried in the program this 
might result in getting the later state, which might not be desired. Consider the following example:

<pre>set(10,10,10,'stone');
scan(10,10,10,0,0,0, b = _);
set(10,10,10,'air');
print(b); // 'air', block was remembered 'lazily', and evaluated by `print`, when it was already set to air
set(10,10,10,'stone');
scan(10,10,10,0,0,0, b = block(_));
set(10,10,10,'air');
print(b); // 'stone', block was evaluated 'eagerly' but call to `block`
</pre>

## World Manipulation

All the functions below can be used with block value, queried with coord triple, or 3-long list. All `pos` in the 
functions referenced below refer to either method of passing block position.

### `set(pos, block, property?, value?, ...)`

First part of the `set` function is either a coord triple, list of three numbers, or other block with coordinates. 
Second part, `block` is either block value as a result of `block()` function string value indicating the block name, 
and optional `property - value` pairs for extra block properties. If `block` is specified only by name, then if a 
destination block is the same the `set` operation is skipped, otherwise is executed, for other potential extra
properties.

The returned value is either the block state that has been set, or `false` if block setting was skipped

<pre>
set(0,5,0,'bedrock')  => bedrock
set(l(0,5,0), 'bedrock')  => bedrock
set(block(0,5,0), 'bedrock')  => bedrock
scan(0,5,0,0,0,0,set(_,'bedrock'))  => 1
set(pos(players()), 'bedrock')  => bedrock
set(0,0,0,'bedrock')  => 0   // or 1 in overworlds generated in 1.8 and before
scan(0,100,0,20,20,20,set(_,'glass'))
    // filling the area with glass
scan(0,100,0,20,20,20,set(_,block('glass')))
    // little bit faster due to internal caching of block state selectors
b = block('glass'); scan(0,100,0,20,20,20,set(_,b))
    // yet another option, skips all parsing
set(x,y,z,'iron_trapdoor')  // sets bottom iron trapdoor
set(x,y,z,'iron_trapdoor[half=top]')  // Incorrect. sets bottom iron trapdoor - no parsing of properties
set(x,y,z,'iron_trapdoor','half','top') // correct - top trapdoor
set(x,y,z, block('iron_trapdoor[half=top]')) // also correct, block() provides extra parsing
set(x,y,z,'hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') // extra block data
</pre>

### `without_updates(expr)`

Evaluates subexpression without causing updates when blocks change in the world

Consider following scenario: We would like to generate a bunch of terrain in a flat world following a perlin noise 
generator. The following code causes a cascading effect as blocks placed on chunk borders will cause other chunks to get 
loaded to full, thus generated:

<pre>
__config() -> m(l('scope', 'global'));
__on_chunk_generated(x, z) -> (
  scan(x,0,z,0,0,0,15,15,15,
    if (perlin(_x/16, _y/8, _z/16) > _y/16,
      set(_, 'black_stained_glass');
    )
  )
)
</pre>

The following addition resolves this issue, by not allowing block updates past chunk borders:

<pre>
__config() -> m(l('scope', 'global'));
__on_chunk_generated(x, z) -> (
  without_updates(
    scan(x,0,z,0,0,0,15,15,15,
      if (perlin(_x/16, _y/8, _z/16) > _y/16,
        set(_, 'black_stained_glass');
      )
    )
  )
)
</pre>

### `place_item(item, pos, facing?, sneak?)`

Uses a given item in the world like it was used by a player. Item names are default minecraft item name, 
less the minecraft prefix. Default facing is 'up', but there are other options: 'down', 'north', 'east', 'south', 
'west', but also there are other secondary directions important for placement of blocks like stairs, doors, etc. 
Try experiment with options like 'north-up' which is placed facing north with cursor pointing to the upper part of the 
block, or 'up-north', which means a block placed facing up (player looking down) and placed smidge away of the block 
center towards north. Optional sneak is a boolean indicating if a player would be sneaking while placing the 
block - this option only affects placement of chests and scaffolding at the moment. 

Works with items that have the right-click effect on the block placed, like `bone_meal` on grass or axes on logs,
but doesn't open chests / containers, so have no effect on interactive blocks, like TNT, comparators, etc.

Returns true if placement/use was 
successful, false otherwise.

<pre>
place_item('stone',x,y,z) // places a stone block on x,y,z block
place_item('piston,x,y,z,'down') // places a piston facing down
place_item('carrot',x,y,z) // attempts to plant a carrot plant. Returns true if could place carrots at that position.
place_item('bone_meal',x,y,z) // attempts to bonemeal the ground.
place_item('wooden_axe',x,y,z) // attempts to strip the log.
</pre>

### `set_poi(pos, type, occupancy?)`

Sets a Point of Interest (POI) of a specified type with optional custom occupancy. By default new POIs are not occupied. 
If type is `null`, POI at position is removed. In any case, previous POI is also removed. Available POI types are:

*   `'unemployed', 'armorer', 'butcher', 'cartographer', 'cleric', 'farmer', 'fisherman', 'fletcher', 'leatherworker', 'librarian', 'mason', 'nitwit', 'shepherd', 'toolsmith', 'weaponsmith', 'home', 'meeting', 'beehive', 'bee_nest', 'nether_portal'`

Interestingly, `unemployed`, and `nitwit` are not used in the game, meaning, they could be used as permanent spatial 
markers for scarpet apps. `meeting` is the only one with increased max occupancy of 32.

### `set_biome(pos, biome_name, update=true)`

Changes the biome at that block position. if update is specified and false, then chunk will not be refreshed
on the clients. Biome changes can only be send to clients with the entire data from the chunk.

### `update(pos)`

Causes a block update at position.

### `block_tick(pos)`

Causes a block to tick at position.

### `random_tick(pos)`

Causes a random tick at position.

### `destroy(pos), destroy(pos, -1), destroy(pos, <N>), destroy(pos, tool, nbt?)`

Destroys the block like it was mined by a player. Add -1 for silk touch, and a positive number for fortune level. 
If tool is specified, and optionally its nbt, it will use that tool and will attempt to mine the block with this tool. 
If called without item context, this function, unlike harvest, will affect all kinds of blocks. If called with item 
in context, it will fail to break blocks that cannot be broken by a survival player.

Without item context it returns `false` if failed to destroy the block and `true` if block breaking was successful. 
In item context, `true` means that breaking item has no nbt to use, `null` indicating that the tool should be 
considered broken in process, and `nbt` type value, for a resulting NBT tag on a hypothetical tool. Its up to the 
programmer to use that nbt to apply it where it belong

Here is a sample code that can be used to mine blocks using items in player inventory, without using player context 
for mining. Obviously, in this case the use of `harvest` would be much more applicable:

<pre>
mine(x,y,z) ->
(
  p = player();
  slot = p~'selected_slot';
  item_tuple = inventory_get(p, slot);
  if (!item_tuple, destroy(x,y,z,'air'); return()); // empty hand, just break with 'air'
  l(item, count, tag) = item_tuple;
  tag_back = destroy(x,y,z, item, tag);
  if (tag_back == false, // failed to break the item
    return(tag_back)
  );
  if (tag_back == true, // block broke, tool has no tag
    return(tag_back)
  );
  if (tag_back == null, //item broke
    delete(tag:'Damage');
    inventory_set(p, slot, count-1, item, tag);
    return(tag_back)
  );
  if (type(tag_back) == 'nbt', // item didn't break, here is the effective nbt
    inventory_set(p, slot, count, item, tag_back);
    return(tag_back)
  );
  print('How did we get here?');
)
</pre>

### `harvest(player, pos)`

Causes a block to be harvested by a specified player entity. Honors player item enchantments, as well as damages the 
tool if applicable. If the entity is not a valid player, no block gets destroyed. If a player is not allowed to break 
that block, a block doesn't get destroyed either.

## Block and World querying

### `pos(block), pos(entity)`

Returns a triple of coordinates of a specified block or entity. Technically entities are queried with `query` function 
and the same can be achieved with `query(entity,'pos')`, but for simplicity `pos` allows to pass all positional objects.

<pre>
pos(block(0,5,0)) => l(0,5,0)
pos(players()) => l(12.3, 45.6, 32.05)
pos(block('stone')) => Error: Cannot fetch position of an unrealized block
</pre>

### `pos_offset(pos, direction, amount?)`

Returns a coords triple that is offset in a specified `direction` by `amount` of blocks. The default offset amount is 
1 block. To offset into opposite facing, use negative numbers for the `amount`.

<pre>
pos_offset(block(0,5,0), 'up', 2)  => l(0,7,0)
pos_offset(l(0,5,0), 'up', -2 ) => l(0,3,0)
</pre>

### `block_properties(pos)`

Returns a list of available block properties for a particular block. If a block has no properties, returns an empty list.

### `property(pos, name)`

Returns property of block at `pos`, or specified by `block` argument. If a block doesn't have that property, `null` 
value is returned. Returned values are always strings. It is expected from the user to know what to expect and convert 
values to numbers using `number()` function or booleans using `bool()` function.

<pre>
set(x,y,z,'iron_trapdoor','half','top'); property(x,y,z,'half')  => top
set(x,y,z,'air'); property(x,y,z,'half')  => null
property(block('iron_trapdoor[half=top]'),'half')  => top
property(block('iron_trapdoor[half=top]'),'powered')  => false
bool(property(block('iron_trapdoor[half=top]'),'powered'))  => 0
</pre>

### `block_data(pos)`

Return NBT string associated with specific location, or null if the block does not carry block data. Can be currently 
used to match specific information from it, or use it to copy to another block

<pre>    block_data(x,y,z) => '{TransferCooldown:0,x:450,y:68, ... }'
</pre>

### `poi(pos), poi(pos, radius), poi(pos, radius, status)`

Queries a POI (Point of Interest) at a given position, returning `null` if none is found, or tuple of poi type and its 
occupancy load. With optional `radius` and `status`, returns a list of POIs around `pos` within a given `radius`. 
If `status` is specified (either 'available', or 'occupied') returns only POIs with that status. The return format is 
again, poi type, occupancy load, and extra tripple of coordinates.

Querying for POIs using the radius is intended use of POI mechanics and ability of accessing individual POIs 
via `poi(pos)` in only provided for completness.

<pre>
poi(x,y,z) => null  // nothing set at position
poi(x,y,z) => ['meeting',3]  // its a bell-type meeting point occupied by 3 villagers
poi(x,y,z,5) => []  // nothing around
poi(x,y,z,5) => [['nether_portal',0,[7,8,9]],['nether_portal',0,[7,9,9]]] // two portal blocks in the range
</pre>

### `biome(pos)`

Returns the biome at that block position.

### `solid(pos)`

Boolean function, true if the block is solid.

### `air(pos)`

Boolean function, true if a block is air... or cave air... or void air... or any other air they come up with.

### `liquid(pos)`

Boolean function, true if the block is liquid, or waterlogged (with any liquid).

### `flammable(pos)`

Boolean function, true if the block is flammable.

### `transparent(pos)`

Boolean function, true if the block is transparent.

### `opacity(pos)`

Numeric function, returning the opacity level of a block.

### `blocks_daylight(pos)`

Boolean function, true if the block blocks daylight.

### `emitted_light(pos)`

Numeric function, returning the light level emitted from the block.

### `light(pos)`

Numeric function, returning the total light level at position.

### `block_light(pos)`

Numeric function, returning the block light at position (from torches and other light sources).

### `sky_light(pos)`

Numeric function, returning the sky light at position (from sky access).

### `see_sky(pos)`

Boolean function, returning true if the block can see sky.

### `hardness(pos)`

Numeric function, indicating hardness of a block.

### `blast_resistance(pos)`

Numeric function, indicating blast_resistance of a block.

### `in_slime_chunk(pos)`

Boolean indicating if the given block position is in a slime chunk.

### `top(type, pos)`

Returns the Y value of the topmost block at given x, z coords (y value of a block is not important), according to the 
heightmap specified by `type`. Valid options are:

*   `light`: topmost light blocking block (1.13 only)
*   `motion`: topmost motion blocking block
*   `terrain`: topmost motion blocking block except leaves
*   `ocean_floor`: topmost non-water block
*   `surface`: topmost surface block

<pre>
top('motion', x, y, z)  => 63
top('ocean_floor', x, y, z)  => 41
</pre>

### `suffocates(pos)`

Boolean function, true if the block causes suffocation.

### `power(pos)`

Numeric function, returning redstone power level at position.

### `ticks_randomly(pos)`

Boolean function, true if the block ticks randomly.

### `blocks_movement(pos)`

Boolean function, true if the block at position blocks movement.

### `block_sound(pos)`

Returns the name of sound type made by the block at position. One of:

*   `wood`
*   `gravel`
*   `grass`
*   `stone`
*   `metal`
*   `glass`
*   `wool`
*   `sand`
*   `snow`
*   `ladder`
*   `anvil`
*   `slime`
*   `sea_grass`
*   `coral`

### `material(pos)`

Returns the name of material of the block at position. very useful to target a group of blocks. One of:

*   `air`
*   `void`
*   `portal`
*   `carpet`
*   `plant`
*   `water_plant`
*   `vine`
*   `sea_grass`
*   `water`
*   `bubble_column`
*   `lava`
*   `snow_layer`
*   `fire`
*   `redstone_bits`
*   `cobweb`
*   `redstone_lamp`
*   `clay`
*   `dirt`
*   `grass`
*   `packed_ice`
*   `sand`
*   `sponge`
*   `wood`
*   `wool`
*   `tnt`
*   `leaves`
*   `glass`
*   `ice`
*   `cactus`
*   `stone`
*   `iron`
*   `snow`
*   `anvil`
*   `barrier`
*   `piston`
*   `coral`
*   `gourd`
*   `dragon_egg`
*   `cake`

### `map_colour(pos)`

Returns the map colour of a block at position. One of:

*   `air`
*   `grass`
*   `sand`
*   `wool`
*   `tnt`
*   `ice`
*   `iron`
*   `foliage`
*   `snow`
*   `clay`
*   `dirt`
*   `stone`
*   `water`
*   `wood`
*   `quartz`
*   `adobe`
*   `magenta`
*   `light_blue`
*   `yellow`
*   `lime`
*   `pink`
*   `gray`
*   `light_gray`
*   `cyan`
*   `purple`
*   `blue`
*   `brown`
*   `green`
*   `red`
*   `black`
*   `gold`
*   `diamond`
*   `lapis`
*   `emerald`
*   `obsidian`
*   `netherrack`
*   `white_terracotta`
*   `orange_terracotta`
*   `magenta_terracotta`
*   `light_blue_terracotta`
*   `yellow_terracotta`
*   `lime_terracotta`
*   `pink_terracotta`
*   `gray_terracotta`
*   `light_gray_terracotta`
*   `cyan_terracotta`
*   `purple_terracotta`
*   `blue_terracotta`
*   `brown_terracotta`
*   `green_terracotta`
*   `red_terracotta`
*   `black_terracotta`


### `loaded(pos)`

Boolean function, true if the block is accessible for the game mechanics. Normally `scarpet` doesn't check if operates 
on loaded area - the game will automatically load missing blocks. We see this as an advantage. Vanilla `fill/clone` 
commands only check the specified corners for loadness.

To check if a block is truly loaded, I mean in memory, use `generation_status(x) != null`, as chunks can still be loaded 
outside of the playable area, just are not used by any of the game mechanic processes.

<pre>
loaded(pos(players()))  => 1
loaded(100000,100,1000000)  => 0
</pre>

### `(Deprecated) loaded_ep(pos)`

Boolean function, true if the block is loaded and entity processing, as per 1.13.2

Deprecated as of scarpet 1.6, use `loaded_status(x) > 0`, or just `loaded(x)` with the same effect

### `loaded_status(pos)`

Returns loaded status as per new 1.14 chunk ticket system, 0 for inaccessible, 1 for border chunk, 2 for redstone ticking, 
3 for entity ticking

### `is_chunk_generated(pos)`, `is_chunk_generated(pos, force)`

Returns `true` if the region file for the chunk exists, 
`false` otherwise. If optional force is `true` it will also check if the chunk has a non-empty entry in its region file
Can be used to assess if the chunk has been touched by the game or not.

`generation_status(pos, false)` only works on currently loaded chunks, and `generation_status(pos, true)` will create
an empty loaded chunk, even if it is not needed, so `is_chunk_generated` can be used as a efficient proxy to determine
if the chunk physically exists.

Running `is_chunk_generated` is has no effects on the world, but since it is an external file operation, it is
considerably more expensive (unless area is loaded) than other generation and loaded checks.

### `generation_status(pos), generation_status(pos, true)`

Returns generation status as per the ticket system. Can return any value from several available but chunks 
can only be stable in a few states: `full`, `features`, `liquid_carvers`, and `structure_starts`. Returns `null` 
if the chunk is not in memory unless called with optional `true`.

### `inhabited_time(pos)`

Returns inhabited time for a chunk.

### `spawn_potential(pos)`

Returns spawn potential at a location (1.16+ only)

### `structure_eligibility(pos, ?structure, ?size_needed)`

Checks wordgen eligibility for a structure in a given chunk. If no structure is given, or `null`, then it will check
 for all structures. If bounding box of the structures is also requested, it will compute size of potential
  structures. This function, unlike other in the `structure*` category is not using world data nor accesses chunks
  making it preferred for scoping ungenerated terrain, but it takes some compute resources to calculate the structure.
  
  Unlike `'structure'` this will return a tentative structure location. Random factors in world generation may prevent
  the actual structure from forming.
  
If structure is specified, it will return `null` if a chunk is not eligible, `true` if the structure should appear, or 
a map with two values: `'box'` for a pair of coordinates indicating bounding box of the structure, and `'pieces'` for 
list of elements of the structure (as a tuple), with its name, direction, and box coordinates of the piece.

If structure is not specified, it will return a set of structure names that are eligible, or a map with structures
as keys, and same type of map values as with a single structure call. An empty set or an empty map would indicate that nothing
should be generated there.


### `structures(pos), structures(pos, structure_name)`

Returns structure information for a given block position. Note that structure information is the same for all the 
blocks from the same chunk. `structures` function can be called with a block, or a block and a structure name. In 
the first case it returns a map of structures at a given position, keyed by structure name, with values indicating 
the bounding box of the structure - a pair of two 3-value coords (see examples). When called with an extra structure 
name, returns a map with two values, `'box'` for bounding box of the structure, and `'pieces'` for a list of 
components for that structure, with their name, direction and two sets of coordinates 
indicating the bounding box of the structure piece.

### `structure_references(pos), structure_references(pos, structure_name)`

Returns structure information that a chunk with a given block position is part of. `structure_references` function 
can be called with a block, or a block and a structure name. In the first case it returns a list of structure names 
that give chunk belongs to. When called with an extra structure name, returns list of positions pointing to the 
lowest block position in chunks that hold structure starts for these structures. You can query that chunk structures 
then to get its bounding boxes.

### `set_structure(pos, structure_name), set_structure(pos, structure_name, null)`

Creates or removes structure information of a structure associated with a chunk of `pos`. Unlike `plop`, blocks are 
not placed in the world, only structure information is set. For the game this is a fully functional structure even 
if blocks are not set. To remove the structure a given point is in, use `structure_references` to find where current 
structure starts.

### `plop(pos, what)`

Plops a structure or a feature at a given `pos`, so block, triple position coordinates or a list of coordinates. 
To `what` gets plopped and exactly where it often depends on the feature or structure itself. For example, all 
structures are chunk aligned, and often span multiple chunks. Repeated calls to plop a structure in the same chunk 
would result either in the same structure generated on top of each other, or with different state, but same position. 
Most structures generate at specific altitudes, which are hardcoded, or with certain blocks around them. API will 
cancel all extra position / biome / random requirements for structure / feature placement, but some hardcoded 
limitations may still cause some of structures/features not to place. Some features require special blocks to be
present, like coral -> water or ice spikes -> snow block, and for some features, like fossils, placement is all sorts 
of messed up. This can be partially avoided for structures by setting their structure information via `set_structure`, 
which sets it without looking into world blocks, and then use `plop` to fill it with blocks. This may, or may not work.

All generated structures will retain their properties, like mob spawning, however in many cases the world / dimension 
itself has certain rules to spawn mobs, like plopping a nether fortress in the overworld will not spawn nether mobs, 
because nether mobs can spawn only in the nether, but plopped in the nether - will behave like a valid nether fortress.

`plop` will not use world random number generator to generate structures and features, but its own. This has a benefit 
that they will generate properly randomly, not the same time every time.

Structure list:

*   `monument`: Ocean Monument. Generates at fixed Y coordinate, surrounds itself with water.
*   `fortress`: Nether Fortress. Altitude varies, but its bounded by the code.
*   `mansion`: Woodland Mansion
*   `jungle_temple`: Jungle Temple
*   `desert_temple`: Desert Temple. Generates at fixed Y altitude.
*   `end_city`: End City with Shulkers
*   `igloo`: Igloo
*   `shipwreck`: Shipwreck, version1?
*   `shipwreck2`: Shipwreck, version2?
*   `witch_hut`
*   `ocean_ruin`, `ocean_ruin_small`, `ocean_ruin_tall`: Stone variants of ocean ruins.
*   `ocean_ruin_warm`, `ocean_ruin_warm_small`, `ocean_ruin_warm_tall`: Sandstone variants of ocean ruins.
*   `treasure`: A treasure chest. Yes, its a whole structure.
*   `pillager_outpost`: A pillager outpost.
*   `mineshaft`: A mineshaft.
*   `mineshaft_mesa`: A Mesa (Badlands) version of a mineshaft.
*   `village`: Plains, oak village.
*   `village_desert`: Desert, sandstone village.
*   `village_savanna`: Savanna, acacia village.
*   `village_taiga`: Taiga, spruce village.
*   `village_snowy`: Resolute, Canada.
*   `nether_fossil`: Pile of bones (1.16)
*   `ruined_portal`: Ruined portal, random variant.
*   `bastion_remnant`: Piglin bastion, random variant for the chunk (1.16)
*   `bastion_remnant_housing`: Housing units version of a piglin bastion (1.16)
*   `bastion_remnant_stable`: Hoglin stables version of q piglin bastion (1.16)
*   `bastion_remnant_treasure`: Treasure room version of a piglin bastion (1.16)
*   `bastion_remnant_bridge` : Bridge version of a piglin bastion (1.16)

Feature list:

*   `oak`
*   `oak_beehive`: oak with a hive (1.15+).
*   `oak_large`: oak with branches.
*   `oak_large_beehive`: oak with branches and a beehive (1.15+).
*   `birch`
*   `birch_large`: tall variant of birch tree.
*   `shrub`: low bushes that grow in jungles.
*   `shrub_acacia`: low bush but configured with acacia (1.14 only)
*   `shrub_snowy`: low bush with white blocks (1.14 only)
*   `jungle`: a tree
*   `jungle_large`: 2x2 jungle tree
*   `spruce`
*   `spruce_large`: 2x2 spruce tree
*   `pine`: spruce with minimal leafage (1.15+)
*   `pine_large`: 2x2 spruce with minimal leafage (1.15+)
*   `spruce_matchstick`: see 1.15 pine (1.14 only).
*   `spruce_matchstick_large`: see 1.15 pine_large (1.14 only).
*   `dark_oak`
*   `acacia`
*   `oak_swamp`: oak with more leaves and vines.
*   `well`: desert well
*   `grass`: a few spots of tall grass
*   `grass_jungle`: little bushier grass feature (1.14 only)
*   `lush_grass`: grass with patchy ferns (1.15+)
*   `tall_grass`: 2-high grass patch (1.15+)
*   `fern`: a few random 2-high ferns
*   `cactus`: random cacti
*   `dead_bush`: a few random dead bushi
*   `fossils`: underground fossils, placement little wonky
*   `mushroom_brown`: large brown mushroom.
*   `mushroom_red`: large red mushroom.
*   `ice_spike`: ice spike. Require snow block below to place.
*   `glowstone`: glowstone cluster. Required netherrack above it.
*   `melon`: a patch of melons
*   `melon_pile`: a pile of melons (1.15+)
*   `pumpkin`: a patch of pumpkins
*   `pumpkin_pile`: a pile of pumpkins (1.15+)
*   `sugarcane`
*   `lilypad`
*   `dungeon`: Dungeon. These are hard to place, and fail often.
*   `iceberg`: Iceberg. Generate at sea level.
*   `iceberg_blue`: Blue ice iceberg.
*   `lake`
*   `lava_lake`
*   `end_island`
*   `chorus`: Chorus plant. Require endstone to place.
*   `sea_grass`: a patch of sea grass. Require water.
*   `sea_grass_river`: a variant.
*   `kelp`
*   `coral_tree, coral_mushroom, coral_claw`: various coral types, random color.
*   `coral`: random coral structure. Require water to spawn.
*   `sea_pickle`
*   `boulder`: A rocky, mossy formation from a giant taiga biome. Doesn't update client properly, needs relogging.
*   `crimson_fungus` (1.16)
*   `warped_fungus` (1.16)
*   `nether_sprouts` (1.16)
*   `crimson_roots` (1.16)
*   `warped_roots`  (1.16)
*   `weeping_vines` (1.16)
*   `twisting_vines` (1.16)
*   `basalt_pillar` (1.16)

### `reload_chunk(pos)`

Sends full chunk data to clients. Useful when lots stuff happened and you want to refresh it on the clients.

### `reset_chunk(pos)`, `reset_chunk(from_pos, to_pos)`, `reset_chunk(l(pos, ...))`
Removes and resets the chunk, all chunks in the specified area or all chunks in a list at once, removing all previous
blocks and entities, and replacing it with a new generation. For all currently loaded chunks, they will be brought
to their current generation status, and updated to the player. All chunks that are not in the loaded area, will only
be generated to the `'structure_starts'` status, allowing to generate them fully as players are visiting them.
Chunks in the area that has not been touched yet by the game will not be generated / regenerated.

It returns a `map` with a report indicating how many chunks were affected, and how long each step took:
 * `requested_chunks`: total number of chunks in the requested area or list
 * `affected_chunks`: number of chunks that will be removed / regenerated
 * `loaded_chunks`: number of currently loaded chunks in the requested area / list
 * `layer_count_<status>`: number of chunks for which a `<status>` generation step has been performed
 * `layer_time_<status>`: cumulative time for all chunks spent on generating `<status>` step
 
### add_chunk_ticket(pos, type, radius)

Adds a chunk ticket at a position, which makes the game to keep the designated area centered around
`pos` with radius of `radius` loaded for a predefined amount of ticks, defined by `type`. Allowed types
are `portal`: 300 ticks, `teleport`: 5 ticks, and `unknown`: 1 tick. Radius can be from 1 to 32 ticks.

This function is tentative - will likely change when chunk ticket API is properly fleshed out.