# Scarpet events system

Provides the ability to execute specific function whenever an event occurs. The functions to be registered need to 
conform with the arguments to the event specification. When loading module functions, each function that starts 
with `__on_...` and has the required arguments, will be bound automatically. In case of player specific modules, 
all player action events will be directed to the appropriate player space, and all tick events will be executed in 
the global context, so its not a good idea to mix these two, so use either of these, or use commands to call tick 
events directly, or handle player specific data inside a module. In near future using global events 
(on_tick, on_chunk_generated) will be not allowed in player scoped apps.

### Event list

Here is a list of events that can be handled by scarpet. This list includes prefixes required by modules to autoload 
them, but you can add any function to any event if it accepts required number of parameters:

<pre>
// world callbacks
__on_tick()         // can access blocks and entities in the overworld
__on_tick_nether()  // can access blocks and entities in the nether
__on_tick_ender()   // can access blocks and entities in the end
__on_chunk_generated(x,z) // called after a chunk is promoted to the full chunk,
                          // providing lowest x and z coords in the chunk
                          // event will not work with optifine installed in the game
__on_lightning(block, mode) // mode is `true` if lightning caused horse trap to spawn
// player specific callbacks
__on_player_uses_item(player, item_tuple, hand)  // right click action
__on_player_releases_item(player, item_tuple, hand)  // client action (e.g. bow)
__on_player_finishes_using_item(player, item_tuple, hand))  // server action (e.g. food), called item is from before it is used.
__on_player_clicks_block(player, block, face)  // left click attack on a block
__on_player_breaks_block(player, block) // called after block is broken (the caller receives previous blockstate)
__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)  // player right clicks block with anything
__on_player_interacts_with_block(player, hand, block, face, hitvec)  //right click on a block resulted in activation of said block
__on_player_places_block(player, item_tuple, hand, block) // player have just placed the block.
__on_player_interacts_with_entity(player, entity, hand)
__on_player_chooses_recipe(player, recipe, full_stack)
__on_player_switches_slot(player, from, to)
__on_player_attacks_entity(player, entity)
__on_player_takes_damage(player, amount, source, source_entity)
__on_player_deals_damage(player, amount, entity)
__on_player_dies(player)
__on_player_respawns(player)
__on_player_changes_dimension(player, from_pos, from_dimension, to_pos, to_dimension)
__on_player_rides(player, forward, strafe, jumping, sneaking)
__on_player_jumps(player)
__on_player_deploys_elytra(player)
__on_player_wakes_up(player)
__on_player_starts_sneaking(player)
__on_player_stops_sneaking(player)
__on_player_starts_sprinting(player)
__on_player_stops_sprinting(player)
__on_player_drops_item(player)
__on_player_drops_stack(player)
__on_player_connects(player)
__on_player_disconnects(player, reason)
__on_statistic(player, category, event, value) // player statistic changes
</pre>

Couple special cases.
* `__on_player_changes_dimension` returns `null` as `to_pos` when player goes back to the overworld from the end
, since the respawn location of the player is not control by the teleport, or a player can see the end credits. After
 the player is eligible to respawn in the overworld, `__on_player_respawns` will be triggered.

### `/script event` command

used to display current events and bounded functions. use `add_to` ro register new event, or `remove_from` to 
unbind a specific function from an event. Function to be bounded to an event needs to have the same number of 
parameters as the action is attempting to bind to (see list above). All calls in modules loaded via `/script load` 
that have functions listed above will be automatically bounded and unbounded when script is unloaded.
