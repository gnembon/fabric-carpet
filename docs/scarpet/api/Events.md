# Scarpet events system

Provides the ability to execute specific function whenever an event occurs. The functions to be registered need to 
conform with the arguments to the event specification. When loading module functions, each function that starts 
with `__on_...` and has the required arguments, will be bound automatically. In case of player specific modules, 
all player action events will be directed to the appropriate player space, and all tick events will be executed in 
the global context, so its not a good idea to mix these two, so use either of these, or use commands to call tick 
events directly, or handle player specific data inside an app.

Most events strive to report right before they take an effect in the game. The purpose of that is that this give a choice
for the programmer to handle them right away (as it happens, potentially affect the course of action by changing the
environment right before it), or decide to handle it after by scheduling another call for the end of the tick. Or both - 
partially handle the event before it happens and handle the rest after. While in some cases this may lead to programmers
confusion (like handling the respawn event still referring to player's original position and dimension), but gives much 
more control over these events.

## Event list

Here is a list of events that are handled by scarpet. This list includes prefixes for function names, allowing apps
to autoload them, but you can always add any function to any event (using `/script event` command)
if it accepts required number of parameters.

## Global events

Handling global events is only allowed in apps with `'global'` scope. With the default scope (`'player'`) you
simply wouldn't know which player to hook up this event to. All other events can be handled by any app type.

### `__on_tick()`
Event triggers at the beginning of each tick, located in the overworld. You can use `in_dimension()`
to access other dimensions from there.

### `__on_tick_nether()` (Deprecated)
Duplicate of `tick`, just automatically located in the nether.

### `__on_tick_ender()` (Deprecated)
Duplicate of `tick`, just automatically located in the end.

### `__on_chunk_generated(x,z)`
Called right after a chunk at a given coordinate is full generated. `x` and `z` correspond
to the lowest x and z coords in the chunk. Event may (or may not) work with Optifine installed
at the same time.

### `__on_lightning(block, mode)`
Triggered right after a lightning strikes. Lightning entity as well as potential horseman trap would 
already be spawned at that point. `mode` is `true` if the lightning did cause a trap to spawn. 
 
## Player events

These are triggered with a player context. For apps with a 'player' scope, they trigger once for the appropriate
player. In apps with `global` scope they trigger once for all players.

### `__on_player_uses_item(player, item_tuple, hand)`
Triggers with a right click action. Event is triggered right after a server receives the packet, before the 
game manages to do anything about it. Event triggers when player starts eating food, or starts drawing a bow.
Use `player_finishes_using_item`, or `player_releases_item` to capture the end of these events.

Event is not triggered when a player places a block, for that use
`player_right_clicks_block` or `player_places_block` event.

### `__on_player_releases_item(player, item_tuple, hand)`
Player stops right-click-holding on an item that can be held. This event is a result of a client request.
Example events that may cause it to happen is releasing a bow. The event is triggered after the game processes
the request, however the `item_tuple` is provided representing the item that the player started with. You can use that and
compare with the currently held item for a delta.

### `__on_player_finishes_using_item(player, item_tuple, hand))`
Player using of an item is done. This is controlled server side and is responsible for such events as finishing
eating. The event is triggered after confirming that the action is valid, and sending the feedback back
to the client, but before triggering it and its effects in game.

### `__on_player_clicks_block(player, block, face)`
Representing left-click attack on a block, usually signifying start of breaking of a block. Triggers right after the server
receives a client packet, before anything happens on the server side.   
  

### `__on_player_breaks_block(player, block)`
Called when player breaks a block, right before any changes to the world are done, but the decision is made to remove the block.

### `__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)` 
Called when player right clicks on a block with anything, or interacts with a block. This event is triggered right
before other interaction events, like `'player_interacts_with_block'` or `'player_places_block'`.
 
### `__on_player_interacts_with_block(player, hand, block, face, hitvec)`
Called when player successfully interacted with a block, which resulted in activation of said block,
right after this happened.
  
### `__on_player_places_block(player, item_tuple, hand, block)`
Triggered when player places a block, after block is placed in the world, but before scoreboard is triggered or player inventory
adjusted. 
 
### `__on_player_interacts_with_entity(player, entity, hand)`
Triggered when player right clicks (interacts) with an entity, even if the entity has no vanilla interaction with the player or
the item they are holding. The event is invoked after receiving a packet from the client, before anything happens server side
with that interaction

### `__on_player_collides_with_entity(player, entity)`
Triggered every time a player - entity collisions are calculated, before effects of collisions are applied in the game. 
Useful not only when colliding with living entities, but also to intercept items or XP orbs before they have an effect 
on the player.

### `__on_player_chooses_recipe(player, recipe, full_stack)`
Triggered when a player clicks a recipe in the crafting window from the crafting book, after server received
a client request, but before any items are moved from its inventory to the crafting menu.

### `__on_player_switches_slot(player, from, to)`
Triggered when a player changes their selected hotbar slot. Applied right after the server receives the message to switch 
the slot.

### `__on_player_swaps_hands(player)`
Triggered when a player sends a command to swap their offhand item. Executed before the effect is applied on the server.

### `__on_player_attacks_entity(player, entity)`
Triggered when a player attacks entity, right before it happens server side.

### `__on_player_takes_damage(player, amount, source, source_entity)`
Triggered when a player is taking damage. Event is executed right after potential absorbtion was applied and before
the actual damage is applied to the player. 

### `__on_player_deals_damage(player, amount, entity)`
Triggered when a player deals damage to another entity. Its applied in the same moment as `player_takes_damage` if both
sides of the event are players, and similar for all other entities, just their absorbtion is taken twice, just noone ever 
notices that ¯\_(ツ)_/¯

### `__on_player_dies(player)`
Triggered when a player dies. Player is already dead, so don't revive them then. Event applied before broadcasting messages
about players death and applying external effects (like mob anger etc).

### `__on_player_respawns(player)`
Triggered when a player respawns. This includes spawning after death, or landing in the overworld after leaving the end. 
When the event is handled, a player is still in its previous location and dimension - will be repositioned right after.


### `__on_player_changes_dimension(player, from_pos, from_dimension, to_pos, to_dimension)`
Called when a player moves from one dimension to another. Event is handled still when the player is in its previous
dimension and position.

`player_changes_dimension` returns `null` as `to_pos` when player goes back to the overworld from the end
, since the respawn location of the player is not controlled by the teleport, or a player can still see the end credits. After
 the player is eligible to respawn in the overworld, `player_respawns` will be triggered.

### `__on_player_rides(player, forward, strafe, jumping, sneaking)`
Triggers when a server receives movement controls when riding vehicles. Its handled before the effects are applied
server side.

### `__on_player_jumps(player)`
Triggered when a game receives a jump input from the client, and the player is considered standing on the ground.


### `__on_player_deploys_elytra(player)`
Triggered when a server receives a request to deploy elytra, regardless if the flight was agreed upon server side..

### `__on_player_wakes_up(player)`
Player wakes up from the bed mid sleep, but not when it is kicked out of bed because it finished sleeping.

### `__on_player_starts_sneaking(player)`
### `__on_player_stops_sneaking(player)`
### `__on_player_starts_sprinting(player)`
### `__on_player_stops_sprinting(player)`
Four events triggered when player controls for sneaking and sprinting toggle.

### `__on_player_drops_item(player)`
### `__on_player_drops_stack(player)`
Triggered when the game receives the request from a player to drop one item or full stack from its inventory.
Event happens before anything is changed server side.

### `__on_player_picks_up_item(player, item)`
Triggered AFTER a player successfully ingested an item in its inventory. Item represents the total stack of items
ingested by the player. The exact position of these items is unknown as technically these
items could be spread all across the inventory.

### `__on_player_connects(player)`
Triggered when the player has successfully logged in and was placed in the gaem.

### `__on_player_disconnects(player, reason)`
Triggered when a player sends a disconnect package or is forcefully disconnected from the server.

### `__on_statistic(player, category, event, value)`
Triggered when a player statistic changes. Doesn't notify on periodic an rhythmic events, i.e. 
`time_since_death`, `time_since_rest`, and `played_one_minute` since these are triggered every tick. Event 
is handled before scoreboard values for these statistics are changed.

### `/script event` command

used to display current events and bounded functions. use `add_to` ro register new event, or `remove_from` to 
unbind a specific function from an event. Function to be bounded to an event needs to have the same number of 
parameters as the action is attempting to bind to (see list above). All calls in modules loaded via `/script load` 
that have functions listed above will be automatically bounded and unbounded when script is unloaded.
