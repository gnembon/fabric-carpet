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

### `entity_list(type)`

Returns global lists of entities in the current dimension of a specified type. Currently the following 
selectors are available:

*   `*`: all entites
*   `living`
*   `items`
*   `players`
*   `!players`

### `entity_area(type, cx, cy, cz, dx, dy, dz)`

Returns entities of a specified type in an area centered on `cx, cy, cz` and at most `dx, dy, dz` blocks away from 
the center point. Uses the same selectors as `entities_list`.

### `entity_selector(selector)`

Returns entities satisifying given vanilla entity selector. Most complex among all the methods of selecting entities, 
but the most capable. Selectors are cached so it should be as fast as other methods of selecting entities.

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

### `query(e, 'pitch'), query(e, 'yaw')`

Pitch and Yaw or where entity is looking.

### `query(e, 'look')`

Returns a 3d vector where the entity is looking.

### `query(e, 'motion')`

Triple of entity's motion vector, `l(motion_x, motion_y, motion_z)`

### `query(e, 'motion_x'), query(e, 'motion_y'), query(e, 'motion_z')`

Respective component of the entity's motion vector

### `query(e, 'name'), query(e, 'display_name'), query(e, 'custom_name'), query(e, 'type')`

String of entity name

<pre>
query(e,'name')  => Leatherworker
query(e,'custom_name')  => null
query(e,'type')  => villager
</pre>

## `query(e, 'command_name')`

Returns a valid string to be used in commands to address an entity. Its UUID for all entities except
player, where its their name.

<pre>
run('/kill ' + e~'command_name');
</pre>

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

Breeding age of passive entity, in ticks. If negative, time to adulthood, if positive, breeding cooldown

### `query(e, 'despawn_timer')`

For living entities, the number of ticks they fall outside of immediate player presence.

### `query(e, 'portal_cooldown')`

Number of ticks remaining until an entity can use a portal again.

### `query(e, 'portal_timer')`

Number of ticks an entity sits in a portal.

### `query(e, 'item')`

The item triple (name, count, nbt) if its an item entity, `null` otherwise

### `query(e, 'count')`

Number of items in a stack from item entity, `null` otherwise

### `query(e, 'pickup_delay')`

Retrieves pickup delay timeout for an item entity, `null` otherwise

### `query(e, 'is_baby')`

Boolean, true if its a baby.

### `query(e, 'target')`

Returns mob's attack target or null if none or not applicable.

### `query(e, 'home')`

Returns creature's home position or null if none or not applicable.

### `query(e, 'pose')`

Returns a pose of an entity, one of the following options
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
Returns a lowercase string containing the category of the entity (hostile, passive, water, ambient, misc)

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

### `query(e, 'facing', order?)`

Returns where the entity is facing. optional order (number from 0 to 5, and negative), indicating primary directions 
where entity is looking at. From most prominent (order 0) to opposite (order 5, or -1)

### `query(e, 'trace', reach?, options?...)`

Returns the result of ray tracing from entity perspective, indicating what it is looking at. Default reach is 4.5 
blocks (5 for creative players), and by default it traces for blocks and entities, identical to player attack tracing 
action. This can be customized with `options`, use 'blocks' to trace for blocks, 'liquids' to include liquid blocks 
as possible results, and 'entities' to trace entities. Any combination of the above is possible. When tracing 
entities and blocks, blocks will take over the priority even if transparent or non-colliding 
(aka fighting chickens in tall grass).

Regardless of the options selected, the result could be `null` if nothing is in reach, entity, if look targets an
entity, and block value if block is in reach.

### `query(e, 'nbt', path?)`

Returns full NBT of the entity. If path is specified, it fetches only the portion of the NBT that corresponds to the 
path. For specification of `path` attribute, consult vanilla `/data get entity` command.

Note that calls to `nbt` are considerably more expensive comparing to other calls in Minecraft API, and should only 
be used when there is no other option. Returned value is of type `nbt`, which can be further manipulated with nbt 
type objects via `get, put, has, delete`, so try to use API calls first for that.

## Entity Modification

Like with entity querying, entity modifications happen through one function. Most position and movements modifications 
don't work currently on players as their position is controlled by clients.

Currently there is no ability to modify NBT directly, but you could always use `run('data modify entity ...')`

### `modify(e, 'remove')`

Removes (not kills) entity from the game.

### `modify(e, 'kill')`

Kills the entity.

### `modify(e, 'pos', x, y, z), modify(e, 'pos', l(x,y,z) )`

Moves the entity to a specified coords.

### `modify(e, 'location', x, y, z, yaw, pitch), modify(e, 'location', l(x, y, z, yaw, pitch) )`

Changes full location vector all at once.

### `modify(e, 'x', x), modify(e, 'y', y), modify(e, 'z', z)`

Moves the entity in one direction.

### `modify(e, 'pitch', pitch), modify(e, 'yaw', yaw)`

Changes entity's pitch or yaw.

### `modify(e, 'move', x, y, z), modify(e, 'move', l(x,y,z) )`

Moves the entity by a vector from its current location.

### `modify(e, 'motion', x, y, z), modify(e, 'motion', l(x,y,z) )`

Sets the motion vector (where and how much entity is moving).

### `modify(e, 'motion_z', x), modify(e, 'motion_y', y), modify(e, 'motion_z', z)`

Sets the corresponding component of the motion vector.

### `modify(e, 'accelerate', x, y, z), modify(e, 'accelerate', l(x, y, z) )`

Adds a vector to the motion vector. Most realistic way to apply a force to an entity.

### `modify(e, 'custom_name'), modify(e, 'custom_name', name)`

Sets the custom name of the entity.

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

### `modify(e, 'effect', name, duration?, amplifier?, show_particles?, show_icon?)`

Applies status effect to the living entity. Takes several optional parameters, which default to `0`, `true` 
and `true`. If no duration is specified, or if it's null or 0, the effect is removed.

### `modify(e, 'home', null), modify(e, 'home', block, distance?), modify(e, 'home', x, y, z, distance?)`

Sets AI to stay around the home position, within `distance` blocks from it. `distance` defaults to 16 blocks. 
`null` removes it. _May_ not work fully with mobs that have this AI built in, like Villagers.

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

### `modify(e, 'jump'))`

Will make the entity jump once.

### `modify(e, 'silent', boolean)`

Silences or unsilences the entity.

### `modify(e, 'gravity', boolean)`

Toggles gravity for the entity.

### `modify(e, 'fire', ticks)`

Will set entity on fire for `ticks` ticks. Set to 0 to extinguish.

## Entity Events

There is a number of events that happen to entities that you can attach your own code to in the form of event handlers. 
The event handler is any function that runs in your package that accepts certain expected parameters, which you can 
expand with your own arguments. When it comes to the moment when the given command needs to be executed, it does so 
providing that number of arguments it accepts is equal number of event arguments, and extra arguments passed when 
defining the callback with `entity_event`

The following events can be handled by entities.

*   `'on_tick'`: executes every tick right before the entity is ticked in the game. Required arguments: `entity`
*   `'on_death'`: executes once when a living entity dies. Required arguments: `entity, reason`
*   `'on_removed'`: execute once when an entity is removed. Required arguments: `entity`
*   `'on_damaged'`: executed every time a living entity is about to receive damage. 
Required arguments: `entity, amount, source, attacking_entity`

It doesn't mean that all entity types will have a chance to execute a given event, but entities will not error 
when you attach inapplicable event to it.

### `entity_event(e, event, call_name, args...)`

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

In this case this will protect a villager from entity damage (zombies, etc.) except players by granting all the 
health back to the villager after being harmed.
