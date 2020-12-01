# Entity API

## Entity Selection

Entities have to be fetched before using them. Entities can also change their state between calls to the script if 
game ticks occur either in between separate calls to the programs, or if the program calls `game_tick` on its own. 
In this case - entities would need to be re-fetched, or the code should account for entities dying.

### `player(), player(type), player(name)`

With no arguments, it returns the calling player or the player closest to the caller. Note that the main context 
will receive `p` variable pointing to this player. With `type` or `name` specified, it will try first to match a type, 
returning a list of players matching a type, and if this fails, will assume its player name query retuning player with 
that name, or `null` if no player was found. With `'all'`, list of all players in the game, in all dimensions, so end 
user needs to be cautious, that you might be referring to wrong blocks and entities around the player in question. 
With `type = '*'` it returns all players in caller dimension, `'survival'` returns all survival and adventure players,
`'creative'` returns all creative players, `'spectating'` returns all spectating players, and `'!spectating'`, 
all not-spectating players. If all fails, with `name`, the player in question, if he/she is logged in.

### `entity_id(uuid), entity_id(id)`

Fetching entities either by their ID obtained via `entity ~ 'id'`, which is unique for a dimension and current world 
run, or by UUID, obtained via `entity ~ 'uuid'`. It returns null if no such entity is found. Safer way to 'store' 
entities between calls, as missing entities will be returning `null`. Both calls using UUID or numerical ID are `O(1)`, 
but obviously using UUIDs takes more memory and compute.

### `entity_list(descriptor)`

Returns global lists of entities in the current dimension matching specified descriptor.
Calls to `entity_list` always fetch entities from the current world that the script executes.
 
### `entity_types(descriptor)`

Resolves a given descriptor returning list of entity types that match it. The returned list of types is also a valid list
of descriptors that can be use elsewhere where entity types are required. 

Currently, the following descriptors are available:

*  `*`: all entities, even `!valid`, matches all entity types.
*  `valid` - all entities that are not dead (health > 0). All main categories below also return only 
entities in the `valid` category. matches all entity types. `!valid` matches all entites that are already dead of all types.
*  `living` - all entities that resemble a creature of some sort
*  `projectile` - all entities or types that are not living that can be throw or projected, `!projectile` matches all types that
   are not living, but cannot the thrown or projected.
*  `minecarts` matches all minecart types. `!minecarts` matches all types that are not live, but also not minecarts. Using plural
since `minecart` is a proper entity type on its own.
*  `undead`, `arthropod`, `aquatic`, `regular`, `illager` - all entities / types that belong to any of these groups. All 
living entities belong to one and only one of these. Corresponding negative (e.g. `!undead`) corresponds to all mobs that are 
living but don't belong to that group. Entity groups are used in interaction / battle mechanics like smite for undead, or impaling
for aquatic. Also certain mechanics interact with groups, like ringing a bell with illagers. All other mobs that don't have any of these traits belong
to the `regular` group.
*  `monster`, `creature`, `ambient`, `water_creature`, `water_ambient`, `misc` - another categorization of 
living entities based on their spawn group. Negative descriptor resolves to all living types that don't belong to that
category.
*  Any of the following standard entity types (equivalent to selection from `/summon` vanilla command: 
`area_effect_cloud`, `armor_stand`, `arrow`, `bat`, `bee`, `blaze`, `boat`, `cat`, `cave_spider`, `chest_minecart`, 
`chicken`, `cod`, `command_block_minecart`, `cow`, `creeper`, `dolphin`, `donkey`, `dragon_fireball`, `drowned`, 
`egg`, `elder_guardian`, `end_crystal`, `ender_dragon`, `ender_pearl`, `enderman`, `endermite`, `evoker`, 
`evoker_fangs`, `experience_bottle`, `experience_orb`, `eye_of_ender`, `falling_block`, `fireball`, `firework_rocket`, 
`fishing_bobber`, `fox`, `furnace_minecart`, `ghast`, `giant`, `guardian`, `hoglin`, `hopper_minecart`, `horse`, 
`husk`, `illusioner`, `iron_golem`, `item`, `item_frame`, `leash_knot`, `lightning_bolt`, `llama`, `llama_spit`, 
`magma_cube`, `minecart`, `mooshroom`, `mule`, `ocelot`, `painting`, `panda`, `parrot`, `phantom`, `pig`, `piglin`, 
`piglin_brute`, `pillager`, `player`, `polar_bear`, `potion`, `pufferfish`, `rabbit`, `ravager`, `salmon`, `sheep`, 
`shulker`, `shulker_bullet`, `silverfish`, `skeleton`, `skeleton_horse`, `slime`, `small_fireball`, `snow_golem`, 
`snowball`, `spawner_minecart`, `spectral_arrow`, `spider`, `squid`, `stray`, `strider`, `tnt`, `tnt_minecart`, 
`trader_llama`, `trident`, `tropical_fish`, `turtle`, `vex`, `villager`, `vindicator`, `wandering_trader`, `witch`, 
`wither`, `wither_skeleton`, `wither_skull`, `wolf`, `zoglin`, `zombie`, `zombie_horse`, `zombie_villager`, 
`zombified_piglin`

All categories can be preceded with `'!'` which will fetch all entities (unless otherwise noted) that are valid (health > 0) but not 
belonging to that group. 

### `entity_area(type, center, distance)`

 
Returns entities of a specified type in an area centered on `center` and at most `distance` blocks away from 
the center point. Uses the same `type` selectors as `entities_list`.

`center` and `distance` can either be a triple of coordinates or three consecutive arguments for `entity_area`. `center` can 
also be represented as a block, in this case the search box will be centered on the middle of the block.

entity_area is simpler than `entity_selector` and runs about 20% faster, but is limited to predefined selectors and 
cuboid search area.

### `entity_selector(selector)`

Returns entities satisifying given vanilla entity selector. Most complex among all the methods of selecting entities, 
but the most capable. Selectors are cached so it should be as fast as other methods of selecting entities. Unlike other
entities fetching / filtering method, this one doesn't guarantee to return entities from current dimension, since
selectors can return any loaded entity in the world.

### `spawn(name, pos, nbt?)`

Spawns and places an entity in world, like `/summon` vanilla command. Requires a position to spawn, and optional 
extra nbt data to merge with the entity. What makes it different from calling `run('summon ...')`, is the fact that 
you get the entity back as a return value, which is swell.

## Entity Manipulation

Unlike with blocks, that use a plethora of vastly different querying functions, entities are queried with the `query` 
function and altered via the `modify` function. Type of information needed or values to be modified are different for 
each call.

Using `~` (in) operator is an alias for `query`. Especially useful if a statement has no arguments, 
which in this case can be radically simplified:

<pre>
query(p, 'name') <=> p ~ 'name'     // much shorter and cleaner
query(p, 'holds', 'offhand') <=> p ~ l('holds', 'offhand')    // not really but can be done
</pre>

### `query(e, 'removed')`

Boolean. True if the entity is removed.

### `query(e, 'id')`

Returns numerical id of the entity. Most efficient way to keep track of entites in a script. 
Ids are only unique within current game session (ids are not preserved between restarts), 
and dimension (each dimension has its own ids which can overlap).

### `query(e, 'uuid')`

Returns the UUID (unique id) of the entity. Can be used to access entities with the other vanilla commands and 
remains unique regardless of the dimension, and is preserved between game restarts. Apparently players cannot be 
accessed via UUID, but should be accessed with their name instead.

<pre>
map(entities_area('*',x,y,z,30,30,30),run('kill '+query(_,'id'))) // doesn't kill the player
</pre>

### `query(e, 'pos')`

Triple of the entity's position

### `query(e, 'location')`

Quin-tuple of the entity's position (x, y, and z coords), and rotation (yaw, pitch)

### `query(e, 'x'), query(e, 'y'), query(e, 'z')`

Respective component of entity's coordinates

### `query(e, 'pitch')`, `query(e, 'yaw')`

Pitch and Yaw or where entity is looking.

### `query(e, 'head_yaw')`, `query(e, 'body_yaw')`

Applies to living entites. Sets their individual head and body facing angle.

### `query(e, 'look')`

Returns a 3d vector where the entity is looking.

### `query(e, 'motion')`

Triple of entity's motion vector, `l(motion_x, motion_y, motion_z)`. Motion represents the velocity from all the forces
that exert on the given entity. Things that are not 'forces' like voluntary movement, or reaction from the ground are
not part of said forces.

### `query(e, 'motion_x'), query(e, 'motion_y'), query(e, 'motion_z')`

Respective component of the entity's motion vector

### `query(e, 'on_ground')`

Returns `true` if en entity is standing on firm ground and falling down due to that.

### `query(e, 'name'), query(e, 'display_name'), query(e, 'custom_name'), query(e, 'type')`

String of entity name

<pre>
query(e,'name')  => Leatherworker
query(e,'custom_name')  => null
query(e,'type')  => villager
</pre>

### `query(e, 'command_name')`

Returns a valid string to be used in commands to address an entity. Its UUID for all entities except
player, where its their name.

<pre>
run('/kill ' + e~'command_name');
</pre>

### `query(e, 'persistence')`

Returns if a mob has a persistence tag or not. Returns `null` for non-mob entities.

### `query(e, 'is_riding')`

Boolean, true if the entity is riding another entity.

### `query(e, 'is_ridden')`

Boolean, true if another entity is riding it.

### `query(e, 'passengers')`

List of entities riding the entity.

### `query(e, 'mount')`

Entity that `e` rides.

### `query(e, 'tags')`

List of entity's tags.

### `query(e, 'has_tag',tag)`

Boolean, true if the entity is marked with `tag`.

### `query(e, 'is_burning')`

Boolean, true if the entity is burning.

### `query(e, 'fire')`

Number of remaining ticks of being on fire.

### `query(e, 'silent')`

Boolean, true if the entity is silent.

### `query(e, 'gravity')`

Boolean, true if the entity is affected by gravity, like most entities are.

### `query(e, 'immune_to_fire')`

Boolean, true if the entity is immune to fire.

### `query(e, 'dimension')`

Name of the dimension the entity is in.

### `query(e, 'height')`

Height of the entity in blocks.

### `query(e, 'width')`

Width of the entity in blocks.

### `query(e, 'eye_height')`

Eye height of the entity in blocks.

### `query(e, 'age')`

Age of the entity in ticks, i.e. how long it existed.

### `query(e, 'breeding_age')`

Breeding age of passive entity, in ticks. If negative, time to adulthood, if positive, breeding cooldown.

### `query(e, 'despawn_timer')`

For living entities, the number of ticks they fall outside of immediate player presence.

### `query(e, 'portal_cooldown')`

Number of ticks remaining until an entity can use a portal again.

### `query(e, 'portal_timer')`

Number of ticks an entity sits in a portal.

### `query(e, 'item')`

The item triple (name, count, nbt) if its an item entity, `null` otherwise.

### `query(e, 'count')`

Number of items in a stack from item entity, `null` otherwise.

### `query(e, 'pickup_delay')`

Retrieves pickup delay timeout for an item entity, `null` otherwise.

### `query(e, 'is_baby')`

Boolean, true if its a baby.

### `query(e, 'target')`

Returns mob's attack target or null if none or not applicable.

### `query(e, 'home')`

Returns creature's home position (as per mob's AI, leash etc) or null if none or not applicable.

### `query(e, 'spawn_point')`

Returns position tuple, dimension, spawn angle, and whether spawn is forced, assuming the player has a spawn position. 
Returns `false` if spawn position is not set, and `null` if `e` is not a player.

### `query(e, 'path')`

Returns path of the entity if present, `null` otherwise. The path comprises of list of nodes, each is a list
of block value, node type, penalty, and a boolean indicated if the node has been visited.

### `query(e, 'pose')`

Returns a pose of an entity, one of the following options:
 * `'standing'`
 * `'fall_flying'`
 * `'sleeping'`
 * `'swimming'`
 * `'spin_attack'`
 * `'crouching'`
 * `'dying'`

### `query(e, 'sneaking')`

Boolean, true if the entity is sneaking.

### `query(e, 'sprinting')`

Boolean, true if the entity is sprinting.

### `query(e, 'swimming')`

Boolean, true if the entity is swimming.

### `query(e, 'jumping')`

Boolean, true if the entity is jumping.

### `query(e, 'gamemode')`

String with gamemode, or `null` if not a player.

### `query(e, 'gamemode_id')`

Good'ol gamemode id, or null if not a player.

### `query(e, 'player_type')`

Returns `null` if the argument is not a player, otherwise:

*   `singleplayer`: for singleplayer game
*   `multiplayer`: for players on a dedicated server
*   `lan_host`: for singleplayer owner that opened the game to LAN
*   `lan_player`: for all other players that connected to a LAN host
*   `fake`: any carpet-spanwed fake player
*   `shadow`: any carpet-shadowed real player
*   `realms`: ?

### `query(e, 'category')`
Returns a lowercase string containing the category of the entity (hostile, passive, water, ambient, misc).

### `query(e, 'team')`

Team name for entity, or `null` if no team is assigned.

### `query(e, 'ping')`
    
Player's ping in milliseconds, or `null` if its not a player.

### `query(e, 'permission_level')`

Player's permission level, or `null` if not applicable for this entity.

### `query(e, 'effect', name?)`

Without extra arguments, it returns list of effect active on a living entity. Each entry is a triple of short 
effect name, amplifier, and remaining duration in ticks. With an argument, if the living entity has not that potion active, 
returns `null`, otherwise return a tuple of amplifier and remaining duration.

<pre>
query(p,'effect')  => [[haste, 0, 177], [speed, 0, 177]]
query(p,'effect','haste')  => [0, 177]
query(p,'effect','resistance')  => null
</pre>

### `query(e, 'health')`

Number indicating remaining entity health, or `null` if not applicable.

### `query(e, 'hunger')`
### `query(e, 'saturation')`
### `query(e, 'exhaustion')`

Retrieves player hunger related information. For non-players, returns `null`.

### `query(e, 'absorption')`

Gets the absorption of the player (yellow hearts, e.g when having a golden apple.)

### `query(e,'xp')`
### `query(e,'xp_level')`
### `query(e,'xp_progress')`
### `query(e,'score')`

Numbers related to player's xp. `xp` is the overall xp in the bar, `xp_level` is the levels seen in the hotbar,
`xp_points` is a float between 0 and 1 indicating the percentage of the xp bar filled, and `score` is the score displayed upon death 

### `query(e, 'air')`

Number indicating remaining entity health, or `null` if not applicable.

### `query(e, 'holds', slot?)`

Returns triple of short name, stack count, and NBT of item held in `slot`, or `null` if nothing or not applicable. Available options for `slot` are:

*   `mainhand`
*   `offhand`
*   `head`
*   `chest`
*   `legs`
*   `feet`

If `slot` is not specified, it defaults to the main hand.

### `query(e, 'selected_slot')`

Number indicating the selected slot of entity's inventory. Currently only applicable to players.

### `query(e, 'active_block')`

Returns currently mined block by the player, as registered by the game server.

### `query(e, 'breaking_progress')`

Returns current breaking progress of a current player mining block, or `null`, if no block is mined.
Breaking progress, if not null, is any number 0 or above, while 10 means that the block should already be 
broken by the client. This value may tick above 10, if the client / connection is lagging.

Example:

The following program provides custom breaking times, including nice block breaking animations, including instamine, for
blocks that otherwise would take longer to mine.

[Video demo](https://youtu.be/zvEEuGxgCio)
```py
global_blocks = {
  'oak_planks' -> 0,
  'obsidian' -> 1,
  'end_portal_frame' -> 5,
  'bedrock' -> 10
};
  
__on_player_clicks_block(player, block, face) ->
(
   step = global_blocks:str(block);
   if (step == 0,
      destroy(block, -1); // instamine
   , step != null,
      schedule(0, '_break', player, pos(block), str(block), step, 0);
   )
);

_break(player, pos, name, step, lvl) ->
(
   current = player~'active_block';
   if (current != name || pos(current) != pos, 
      modify(player, 'breaking_progress', null);
   ,
      modify(player, 'breaking_progress', lvl);
      if (lvl >= 10, destroy(pos, -1));
      schedule(step, '_break', player, pos, name, step, lvl+1)
   );
)
```

### `query(e, 'facing', order?)`

Returns where the entity is facing. optional order (number from 0 to 5, and negative), indicating primary directions 
where entity is looking at. From most prominent (order 0) to opposite (order 5, or -1).

### `query(e, 'trace', reach?, options?...)`

Returns the result of ray tracing from entity perspective, indicating what it is looking at. Default reach is 4.5 
blocks (5 for creative players), and by default it traces for blocks and entities, identical to player attack tracing 
action. This can be customized with `options`, use `'blocks'` to trace for blocks, `'liquids'` to include liquid blocks 
as possible results, and `'entities'` to trace entities. You can also specify `'exact'` which returns the actual hit
 coordinate as a triple, instead of a block or entity value. Any combination of the above is possible. When tracing 
entities and blocks, blocks will take over the priority even if transparent or non-colliding 
(aka fighting chickens in tall grass).

Regardless of the options selected, the result could be:
 - `null` if nothing is in reach
 - an entity if look targets an entity
 - block value if block is in reach, or
 - a coordinate triple if `'exact'` option was used and hit was successful.

### `query(e, 'brain', memory)`

Retrieves brain memory for entity. Possible memory units highly depend on the game version. Brain is availalble
for villagers (1.15+) and Piglins, Hoglins, Zoglins and Piglin Brutes (1.16+). If memory is not present or 
not available for the entity, `null` is returned.

Type of the returned value (entity, position, number, list of things, etc) depends on the type of the requested
memory. On top of that, since 1.16, memories can have expiry - in this case the value is returned as a list of whatever
was there, and the current ttl in ticks.

Available retrievable memories for 1.15.2:
* `home`, `job_site`, `meeting_point`, `secondary_job_site`, `mobs`, `visible_mobs`, `visible_villager_babies`,
`nearest_players`, `nearest_visible_player`, `walk_target`, `look_target`, `interaction_target`,
`breed_target`, `path`, `interactable_doors`, `opened_doors`, `nearest_bed`, `hurt_by`, `hurt_by_entity`,
`nearest_hostile`, `hiding_place`, `heard_bell_time`, `cant_reach_walk_target_since`,
`golem_last_seen_time`, `last_slept`, `last_woken`, `last_worked_at_poi`

Available retrievable memories as of 1.16.2:
* `home`, `job_site`, `potential_job_site`, `meeting_point`, `secondary_job_site`, `mobs`, `visible_mobs`,
`visible_villager_babies`, `nearest_players`, `nearest_visible_players`, `nearest_visible_targetable_player`,
`walk_target`, `look_target`, `attack_target`, `attack_cooling_down`, `interaction_target`, `breed_target`,
`ride_target`, `path`, `interactable_doors`, `opened_doors`, `nearest_bed`, `hurt_by`, `hurt_by_entity`, `avoid_target`,
`nearest_hostile`, `hiding_place`, `heard_bell_time`, `cant_reach_walk_target_since`, `golem_detected_recently`, 
`last_slept`, `last_woken`, `last_worked_at_poi`, `nearest_visible_adult`, `nearest_visible_wanted_item`, 
`nearest_visible_nemesis`, `angry_at`, `universal_anger`, `admiring_item`, `time_trying_to_reach_admire_item`,
`disable_walk_to_admire_item`, `admiring_disabled`, `hunted_recently`, `celebrate_location`, `dancing`, 
`nearest_visible_huntable_hoglin`, `nearest_visible_baby_hoglin`, `nearest_targetable_player_not_wearing_gold`,
`nearby_adult_piglins`, `nearest_visible_adult_piglins`, `nearest_visible_adult_hoglins`,
`nearest_visible_adult_piglin`, `nearest_visible_zombiefied`, `visible_adult_piglin_count`,
`visible_adult_hoglin_count`, `nearest_player_holding_wanted_item`, `ate_recently`, `nearest_repellent`, `pacified`


### `query(e, 'nbt', path?)`

Returns full NBT of the entity. If path is specified, it fetches only the portion of the NBT that corresponds to the 
path. For specification of `path` attribute, consult vanilla `/data get entity` command.

Note that calls to `nbt` are considerably more expensive comparing to other calls in Minecraft API, and should only 
be used when there is no other option. Returned value is of type `nbt`, which can be further manipulated with nbt 
type objects via `get, put, has, delete`, so try to use API calls first for that.

## Entity Modification

Like with entity querying, entity modifications happen through one function. Most position and movements modifications 
don't work currently on players as their position is controlled by clients.

Currently there is no ability to modify NBT directly, but you could always use `run('data modify entity ...')`.

### `modify(e, 'remove')`

Removes (not kills) entity from the game.

### `modify(e, 'kill')`

Kills the entity.

### `modify(e, 'pos', x, y, z), modify(e, 'pos', l(x,y,z) )`

Moves the entity to a specified coords.

### `modify(e, 'location', x, y, z, yaw, pitch), modify(e, 'location', l(x, y, z, yaw, pitch) )`

Changes full location vector all at once.

### `modify(e, 'x', x), modify(e, 'y', y), modify(e, 'z', z)`

Changes entity's location in the specified direction.

### `modify(e, 'pitch', angle), modify(e, 'yaw', angle)`

Changes entity's pitch or yaw angle.

### `modify(e, 'head_yaw', angle)`, `modify(e, 'body_yaw', angle)`

For living entities, controls their head and body yaw angle.

### `modify(e, 'move', x, y, z), modify(e, 'move', l(x,y,z) )`

Moves the entity by a vector from its current location.

### `modify(e, 'motion', x, y, z), modify(e, 'motion', l(x,y,z) )`

Sets the motion vector (where and how much entity is moving).

### `modify(e, 'motion_z', x), modify(e, 'motion_y', y), modify(e, 'motion_z', z)`

Sets the corresponding component of the motion vector.

### `modify(e, 'accelerate', x, y, z), modify(e, 'accelerate', l(x, y, z) )`

Adds a vector to the motion vector. Most realistic way to apply a force to an entity.

### `modify(e, 'custom_name')`, `modify(e, 'custom_name', name)`, `modify(e, 'custom_name', name, visible)`

Sets the custom name of the entity. Without arguments - clears current custom name. Optional visible affects
if the custom name is always visible, even through blocks.

### `modify(e, 'persistence', bool?)`

Sets the entity persistence tag to `true` (default) or `false`. Only affects mobs. Persistent mobs
don't despawn and don't count towards the mobcap.

### `modify(e, 'age', number)`

Modifies entity's internal age counter. Fiddling with this will affect directly AI behaviours of complex 
entities, so use it with caution.

### `modify(e, 'pickup_delay', number)`

Sets the pickup delay for the item entity.

### `modify(e, 'breeding_age', number)`

Sets the breeding age for the animal.

### `modify(e, 'despawn_timer', number)`

Sets a custom despawn timer value.

### `modify(e, 'portal_cooldown', number)`

Sets a custom number of ticks remaining until an entity can use a portal again.

### `modify(e, 'portal_timer', number)`

Sets a custom number of ticks an entity sits in a portal.

### `modify(e, 'dismount')`

Dismounts riding entity.

### `modify(e, 'mount', other)`

Mounts the entity to the `other`.

### `modify(e, 'drop_passengers')`

Shakes off all passengers.

### `modify(e, 'mount_passengers', passenger, ? ...), modify(e, 'mount_passengers', l(passengers) )`

Mounts on all listed entities on `e`.

### `modify(e, 'tag', tag, ? ...), modify(e, 'tag', l(tags) )`

Adds tag(s) to the entity.

### `modify(e, 'clear_tag', tag, ? ...), modify(e, 'clear_tag', l(tags) )`

Removes tag(s) from the entity.

### `modify(e, 'talk')`

Make noises.

### `modify(e, 'ai', boolean)`

If called with `false` value, it will disable AI in the mob. `true` will enable it again.

### `modify(e, 'no_clip', boolean)`

Sets if the entity obeys any collisions, including collisions with the terrain and basic physics. Not affecting 
players, since they are controlled client side.

### `modify(e, 'effect', name?, duration?, amplifier?, show_particles?, show_icon?, ambient?)`

Applies status effect to the living entity. Takes several optional parameters, which default to `0`, `true`, 
`true` and `false`. If no duration is specified, or if it's null or 0, the effect is removed. If name is not specified,
it clears all effects.

### `modify(e, 'home', null), modify(e, 'home', block, distance?), modify(e, 'home', x, y, z, distance?)`

Sets AI to stay around the home position, within `distance` blocks from it. `distance` defaults to 16 blocks. 
`null` removes it. _May_ not work fully with mobs that have this AI built in, like Villagers.


### `modify(e, 'spawn_point')`, `modify(e, 'spawn_point', null)`, `modify(e, 'spawn_point', pos, dimension?, angle?, forced?)`

Changes player respawn position to given position, optional dimension (defaults to current player dimension), angle (defaults to 
current player facing) and spawn forced/fixed (defaults to `false`). If `none` or nothing is passed, the respawn point
will be reset (as removed) instead.
  
### `modify(e, 'gamemode', gamemode?), modify(e, 'gamemode', gamemode_id?)`

Modifies gamemode of player to whatever string (case-insensitive) or number you put in.

* 0: survival
* 1: creative
* 2: adventure
* 3: spectator

### `modify(e, 'jumping', boolean)`

Will make the entity constantly jump if set to true, and will stop the entity from jumping if set to false.
Note that jumping parameter can be fully controlled by the entity AI, so don't expect that this will have 
a permanent effect. Use `'jump'` to make an entity jump once for sure.

Requires a living entity as an argument.

### `modify(e, 'jump')`

Will make the entity jump once.

### `modify(e, 'silent', boolean)`

Silences or unsilences the entity.

### `modify(e, 'gravity', boolean)`

Toggles gravity for the entity.

### `modify(e, 'fire', ticks)`

Will set entity on fire for `ticks` ticks. Set to 0 to extinguish.

### `modify(e, 'hunger', value)`
### `modify(e, 'saturation', value)`
### `modify(e, 'exhaustion', value)`

Modifies directly player raw hunger components. Has no effect on non-players

### `modify(e, 'absorption', value)`

Sets the absorption value for the player. Each point is half a yellow heart.

### `modify(e, 'score', value)`

Sets the score of a player. Doesn't affect player xp, and is displayed upon death.

### `modify(e, 'add_xp', value)`
### `modify(e, 'xp_level', value)`
### `modify(e, 'xp_score', value)` 

Manipulates player xp values - that's the method you probably want to use 
to manipulate how much 'xp' an action should give.

### `modify(e, 'air', ticks)`

Modifies entity air

### `modify(e, 'add_exhaustion', value)`

Adds exhaustion value to the current player exhaustion level - that's the method you probably want to use
to manipulate how much 'food' an action costs.

### `modify(e, 'breaking_progress', value)` 

Modifies the breaking progress of a player currently mined block. Value of `null`, `-1` makes it reset. 
Values `0` to `10` will show respective animation of a breaking block. Check `query(e, 'breaking_progress')` for 
examples.

### `modify(e, 'nbt_merge', partial_tag)`

Merges a partial tag into the entity data and reloads the entity from its updated tag. Cannot be applied to players.

### `modify(e, 'nbt', tag)`

Reloads the entity from a supplied tag. Better use a valid entity tag, what can go wrong? Wonder what would happen if you
transplant rabbit's brain into a villager? Cannot be applied to players.

## Entity Events

There is a number of events that happen to entities that you can attach your own code to in the form of event handlers. 
The event handler is any function that runs in your package that accepts certain expected parameters, which you can 
expand with your own arguments. When it comes to the moment when the given command needs to be executed, it does so 
providing that number of arguments it accepts is equal number of event arguments, and extra arguments passed when 
defining the callback with `entity_event`.

The following events can be handled by entities:

*   `'on_tick'`: executes every tick right before the entity is ticked in the game. Required arguments: `entity`
*   `'on_death'`: executes once when a living entity dies. Required arguments: `entity, reason`
*   `'on_removed'`: execute once when an entity is removed. Required arguments: `entity`
*   `'on_damaged'`: executed every time a living entity is about to receive damage.
Required arguments: `entity, amount, source, attacking_entity`

It doesn't mean that all entity types will have a chance to execute a given event, but entities will not error 
when you attach an inapplicable event to it.

### `entity_load_handler(descriptor / descriptors, function)`, `entity_load_handler(descriptor / descriptors, call_name, ... args?)`

Attaches a callback to when any entity matching the following type / types is loaded in the game, allowing to grab a handle
to an entity right when it is loaded to the world without querying them every tick. Callback expects one parameter - the entity.
If callback is `null`, then the current entity handler, if present, is removed. Consecutive calls to `entity_load_handler` will add / subtract
of the currently targeted entity types pool.

Like other global events, calls to `entity_load_handler` can only be attached in apps with global scope. Player scope makes so
that it is not clear which player to use run the load call.

```
// veryfast method of getting rid of all the zombies. Callback is so early, its packets haven't reached yet the clients
// so to save on log errors, removal of mobs needs to be scheduled for later.
entity_load_handler('zombie', _(e) -> schedule(0, _(outer(e)) -> modify(e, 'remove')))

// making all zombies immediately faster and less susceptible to friction of any sort
entity_load_handler('zombie', _(e) -> entity_event(e, 'on_tick', _(e) -> modify(e, 'motion', 1.2*e~'motion')))
```

Word of caution: entities can be loaded with chunks in various states, for instance when a chunk is being generated, this means
that accessing world blocks would cause the game to freeze due to force generating that chunk while generating the chunk. Make
sure to never assume the chunk is ready and use `entity_load_handler` to schedule actions around the loaded entity, 
or manipulate entity directly.

For instance the following handler is safe, as it only accesses the entity directly. It makes all spawned pigmen jump
```
/script run entity_load_handler('zombified_piglin', _(e) -> modify(e, 'motion', 0, 1, 0) )
```
But the following handler, attempting to despawn pigmen that spawn in portals, will cause the game to freeze due to cascading access to blocks that would cause neighbouring chunks 
to force generate, causing also error messages for all pigmen caused by packets send after entity is removed by script.
```
/script run entity_load_handler('zombified_piglin', _(e) -> if(block(pos(e))=='nether_portal', modify(e, 'remove') ) )
```
Easiest method to circumvent these issues is delay the check, which may or may not cause cascade load to happen, but 
will definitely break the infinite chain.
```
/script run entity_load_handler('zombified_piglin', _(e) -> schedule(0, _(outer(e)) -> if(block(pos(e))=='nether_portal', modify(e, 'remove') ) ) )
```
But the best is to perform the check first time the entity will be ticked - giving the game all the time to ensure chunk 
is fully loaded and entity processing, removing the tick handler 
```
/script run entity_load_handler('zombified_piglin', _(e) -> entity_event(e, 'on_tick', _(e) -> ( if(block(pos(e))=='nether_portal', modify(e, 'remove')); entity_event(e, 'on_tick', null) ) ) )
```

### `entity_event(e, event, function)`, `entity_event(e, event, call_name, ... args?)`

Attaches specific function from the current package to be called upon the `event`, with extra `args` carried to the 
original required arguments for the event handler.

<pre>
protect_villager(entity, amount, source, source_entity, healing_player) ->
(
   if(source_entity && source_entity~'type' != 'player',
      modify(entity, 'health', amount + entity~'health' );
      particle('end_rod', pos(entity)+l(0,3,0));
      print(str('%s healed thanks to %s', entity, healing_player))
   )
);
__on_player_interacts_with_entity(player, entity, hand) ->
(
   if (entity~'type' == 'villager',
      entity_event(entity, 'on_damage', 'protect_villager', player~'name')
   )
)
</pre>

In this case this will protect a villager from entity damage (zombies, etc.) except from players by granting all the 
health back to the villager after being harmed.
