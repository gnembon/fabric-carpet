# Scarpet events system

Scarpet provides the ability to execute specific function whenever an event occurs. The functions to be subscribed for an event 
need to conform with the arguments to the event specification. There are several built-in events triggered when certain in-game
events occur, but app designers can create their own events and trigger them across all loaded apps.

When loading the app, each function that starts 
with `__on_<event>` and has the required arguments, will be bound automatically to a corresponding built-in event. '`undef`'ying
of such function would result in unbinding the app from this event. Defining event hanlder via `__on_<event>(... args) -> expr` is
equivalent of defining it via `handle_event('<event>', _(... args) -> expr)`

In case of `player` scoped apps, 
all player action events will be directed to the appropriate player hosts. Global events, like `'tick'`, that don't have a specific
player target will be executed multiple times, once for each player app instance. While each player app instance is independent,
statically defined event handlers will be copied to each players app, but if you want to apply them in more controlled way, 
defining event handlers for each player in `__on_start()` function is preferred.

Most built-in events strive to report right before they take an effect in the game. The purpose of that is that this give a choice
for the programmer to handle them right away (as it happens, potentially affect the course of action by changing the
environment right before it), or decide to handle it after by scheduling another call for the end of the tick. Or both - 
partially handle the event before it happens and handle the rest after. While in some cases this may lead to programmers
confusion (like handling the respawn event still referring to player's original position and dimension), but gives much 
more control over these events.

Some events also provide the ability to cancel minecraft's processing of the event by returning `'cancel'` from the event handler.
This only works for particular events that are triggered before they take an effect in the game.
However, cancelling the event will also stop events from subsequent apps from triggering.
The order of events being executed can be changed by specifying an `'event_priority'` in the app config,
with the highest value being executed first.
Note that cancelling some events might introduce a desynchronization to the client from the server,
creating ghost items or blocks. This can be solved by updating the inventory or block to the client, by using `inventory_set` or `set`.

Programmers can also define their own events and signal other events, including built-in events, and across all loaded apps.

## App scopes and event distribution

Events triggered in an app can result in zero, one, or multiple executions, depending on the type of the event, and the app scope.
 * player targeted events (like `player_breaks_block`) target each app once:
   * for global scoped apps - targets a single app instance and provides `player` as the first argument.
   * for player scoped apps - targets only a given player instance, providing player argument for API consistency, 
     since active player in player scoped apps can always be retrived using `player()`. 
 * global events could be handled by multiple players multiple times (like `explosion`, or `tick`):
   * for global scoped apps - triggered once for the single app instance.
   * for player scoped apps - triggered N times for each player separately, so they can do something with that information
 * custom player targeted events (using `signal_event(<event>, <player>, data)`):
   * for global scoped apps - doesn't trigger at all, since there is no way to pass the required player. 
     To target global apps with player information, use `null` for player target, and add player information to the `data`
   * for player scoped apps - triggers once for the specified player and its app instance
 * custom general events (using `signal_event(<event>, null, data)`) behave same like built-in global events:
   * for global scoped apps - triggers once for the only global instance
   * for player scoped apps - triggers N times, once for each player app instance

## Built-in events

Here is the list of events that are handled by default in scarpet. This list includes prefixes for function names, allowing apps
to register them when the app starts, but you can always add any handler function to any event using `/script event` command,
if it accepts the required number of parameters for the event.

## Meta-events

These events are not controlled / triggered by the game per se, but are important for the flow of the apps, however for all 
intent and purpose can be treated as regular events. Unlike regular events, they cannot be hooked up to with `handle_event()`,
and the apps themselves need to have them defined as distinct function definitions. They also cannot be triggered via `signal_event()`.

### `__on_start()`
Called once per app in its logical execution run. For `'global'` scope apps its executed right after the app is loaded. For
`'player'` scope apps, it is triggered once per player before the app can be used by that player. Since each player app acts
independently from other player apps, this is probably the best location to include some player specific initializations. Static
code (i.e. code typed directly in the app code that executes immediately, outside of function definitions), will only execute once
per app, regardless of scope, `'__on_start()'` allows to reliably call player specific initializations. However, most event handlers
defined in the static body of the app will be copied over to each player scoped instance when they join. 

### `__on_close()`

Called once per app when the app is closing or reloading, right before the app is removed. 
For player scoped apps, its called once per player. Scarpet app engine will attempt to call `'__on_close()'` even if
the system is closing down exceptionally. 
 

## Built-in global events

Global events will be handled once per app that is with `'global'` scope. With `player` scoped apps, each player instance
 will be triggerd once for each player, so a global event may be executed multiple times for such apps.

### `__on_server_starts()`
Event triggers after world is loaded and after all startup apps have started. It won't be triggered with `/reload`.

### `__on_server_shuts_down()`
Event triggers when the server started the shutdown process, before `__on_close()` is executed. Unlike `__on_close()`, it doesn't
trigger with `/reload`.

### `__on_tick()`
Event triggers at the beginning of each tick, located in the overworld. You can use `in_dimension()`
to access other dimensions from there.

### `__on_tick_nether()` (Deprecated)
Duplicate of `tick`, just automatically located in the nether. Use `__on_tick() -> in_dimension('nether', ... ` instead.

### `__on_tick_ender()` (Deprecated)
Duplicate of `tick`, just automatically located in the end. Use `__on_tick() -> in_dimension('end', ... ` instead.

### `__on_chunk_generated(x, z)`
Called right after a chunk at a given coordinate is full generated. `x` and `z` correspond
to the lowest x and z coords in the chunk. Handling of this event is scheduled as an off-tick task happening after the 
chunk is confirmed to be generated and loaded to the game, due to the off-thread chunk loading in the game. So 
handling of this event is not technically guaranteed if the game crashes while players are moving for example, and the game 
decides to shut down after chunk is fully loaded and before its handler is processed in between ticks. In normal operation
this should not happen, but let you be warned.

### `__on_chunk_loaded(x, z)`
Called right after a chunk at a given coordinate is loaded. All newly generated chunks are considered loaded as well.
 `x` and `z` correspond to the lowest x and z coordinates in the chunk.

### `__on_chunk_unloaded(x, z)`
Called right before a chunk at the given coordinates is unloaded. `x` and `z` correspond to the lowest x and z coordinates in the chunk.

### `__on_lightning(block, mode)`
Triggered right after a lightning strikes. Lightning entity as well as potential horseman trap would 
already be spawned at that point. `mode` is `true` if the lightning did cause a trap to spawn. 

### `__on_explosion(pos, power, source, causer, mode, fire)`

Event triggered right before explosion takes place and before has any effect on the world. `source` can be an entity causing
the explosion, and `causer` the entity triggering it,
`mode` indicates block effects: `'none'`, `'break'` (drop all blocks), or `'destroy'` - drop few blocks. Event
is not captured when `create_explosion()` is called.

### `__on_explosion_outcome(pos, power, source, causer, mode, fire, blocks, entities)`
Triggered during the explosion, before any changes to the blocks are done, 
but the decision to blow up is already made and entities are already affected.  
The parameter `blocks` contains the list of blocks that will blow up (empty if `explosionNoBlockDamage` is set to `true`).
The parameter `entities` contains the list of entities that have been affected by the explosion. Triggered even with `create_explosion()`.

### `__on_carpet_rule_changes(rule, new_value)`
Triggered when a carpet mod rule is changed. It includes extension rules, not using default `/carpet` command, 
which will then be namespaced as `namespace:rule`.

### Entity load event -> check in details on `entity_load_handler()`

These will trigger every time an entity of a given type is loaded into the game: spawned, added with a chunks, 
spawned from commands, anything really. Check `entity_load_handler()` in the entity section for details.
 
## Built-in player events

These are triggered with a player context. For apps with a `'player'` scope, they trigger once for the appropriate
player. In apps with `global` scope they trigger once as well as a global event.

### `__on_player_uses_item(player, item_tuple, hand)`
Triggers with a right click action. Event is triggered right after a server receives the packet, before the 
game manages to do anything about it. Event triggers when player starts eating food, or starts drawing a bow.
Use `player_finishes_using_item`, or `player_releases_item` to capture the end of these events.

This event can be cancelled by returning `'cancel'`, which prevents the item from being used.

Event is not triggered when a player places a block, for that use
`player_right_clicks_block` or `player_places_block` event.

### `__on_player_releases_item(player, item_tuple, hand)`
Player stops right-click-holding on an item that can be held. This event is a result of a client request.
Example events that may cause it to happen is releasing a bow. The event is triggered after the game processes
the request, however the `item_tuple` is provided representing the item that the player started with. You can use that and
compare with the currently held item for a delta.

### `__on_player_finishes_using_item(player, item_tuple, hand)`
Player using of an item is done. This is controlled server side and is responsible for such events as finishing
eating. The event is triggered after confirming that the action is valid, and sending the feedback back
to the client, but before triggering it and its effects in game.

This event can be cancelled by returning `'cancel'`, which prevents the player from finishing using the item.

### `__on_player_clicks_block(player, block, face)`
Representing left-click attack on a block, usually signifying start of breaking of a block. Triggers right after the server
receives a client packet, before anything happens on the server side.

This event can be cancelled by returning `'cancel'`, which stops the player from breaking a block.
  

### `__on_player_breaks_block(player, block)`
Called when player breaks a block, right before any changes to the world are done, but the decision is made to remove the block.

This event can be cancelled by returning `'cancel'`, which prevents the block from being placed.

### `__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)` 
Called when player right clicks on a block with anything, or interacts with a block. This event is triggered right
before other interaction events, like `'player_interacts_with_block'` or `'player_places_block'`.

This event can be cancelled by returning `'cancel'`, which prevents the player interaction.
 
### `__on_player_interacts_with_block(player, hand, block, face, hitvec)`
Called when player successfully interacted with a block, which resulted in activation of said block,
right after this happened.

### `__on_player_placing_block(player, item_tuple, hand, block)`
Triggered when player places a block, before block is placed in the world.

This event can be cancelled by returning `'cancel'`, which prevents the block from being placed.
  
### `__on_player_places_block(player, item_tuple, hand, block)`
Triggered when player places a block, after block is placed in the world, but before scoreboard is triggered or player inventory
adjusted. 
 
### `__on_player_interacts_with_entity(player, entity, hand)`
Triggered when player right clicks (interacts) with an entity, even if the entity has no vanilla interaction with the player or
the item they are holding. The event is invoked after receiving a packet from the client, before anything happens server side
with that interaction.

This event can be cancelled by returning `'cancel'`, which prevents the player interacting with the entity.

### `__on_player_trades(player, entity, buy_left, buy_right, sell)`
Triggered when player trades with a merchant. The event is invoked after the server allow the trade, but before the inventory
changes and merchant updates its trade-uses counter.
The parameter `entity` can be `null` if the merchant is not an entity.

### `__on_player_collides_with_entity(player, entity)`
Triggered every time a player - entity collisions are calculated, before effects of collisions are applied in the game. 
Useful not only when colliding with living entities, but also to intercept items or XP orbs before they have an effect 
on the player.

### `__on_player_chooses_recipe(player, recipe, full_stack)`
Triggered when a player clicks a recipe in the crafting window from the crafting book, after server received
a client request, but before any items are moved from its inventory to the crafting menu.

This event can be cancelled by returning `'cancel'`, which prevents the recipe from being moved into the crafting grid.

### `__on_player_switches_slot(player, from, to)`
Triggered when a player changes their selected hotbar slot. Applied right after the server receives the message to switch 
the slot.

### `__on_player_swaps_hands(player)`
Triggered when a player sends a command to swap their offhand item. Executed before the effect is applied on the server.

This event can be cancelled by returning `'cancel'`, which prevents the hands from being swapped.

### `__on_player_swings_hand(player, hand)`
Triggered when a player starts swinging their hand. The event typically triggers after a corresponding event that caused it 
(`player_uses_item`, `player_breaks_block`, etc.), but it triggers also after some failed events, like attacking the air. When
swinging continues as an effect of an action, no new swinging events will be issued until the swinging is stopped.

### `__on_player_attacks_entity(player, entity)`
Triggered when a player attacks entity, right before it happens server side.

This event can be cancelled by returning `'cancel'`, which prevents the player from attacking the entity.

### `__on_player_takes_damage(player, amount, source, source_entity)`
Triggered when a player is taking damage. Event is executed right after potential absorbtion was applied and before
the actual damage is applied to the player. 

This event can be cancelled by returning `'cancel'`, which prevents the player from taking damage.

### `__on_player_deals_damage(player, amount, entity)`
Triggered when a player deals damage to another entity. Its applied in the same moment as `player_takes_damage` if both
sides of the event are players, and similar for all other entities, just their absorbtion is taken twice, just noone ever 
notices that ¯\_(ツ)_/¯

This event can be cancelled by returning `'cancel'`, which prevents the damage from being dealt.

### `__on_player_dies(player)`
Triggered when a player dies. Player is already dead, so don't revive them then. Event applied before broadcasting messages
about players death and applying external effects (like mob anger etc).

### `__on_player_respawns(player)`
Triggered when a player respawns. This includes spawning after death, or landing in the overworld after leaving the end. 
When the event is handled, a player is still in its previous location and dimension - will be repositioned right after. In 
case player died, its previous inventory as already been scattered, and its current inventory will not be copied to the respawned
entity, so any manipulation to player data is
best to be scheduled at the end of the tick, but you can still use its current reference to query its status as of the respawn event.

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

### `__on_player_escapes_sleep(player)`
Same as `player_wakes_up` but only triggered when pressing the ESC button. Not sure why Mojang decided to send that event
twice when pressing escape, but might be interesting to be able to detect that.

### `__on_player_starts_sneaking(player)`
### `__on_player_stops_sneaking(player)`
### `__on_player_starts_sprinting(player)`
### `__on_player_stops_sprinting(player)`
Four events triggered when player controls for sneaking and sprinting toggle.

### `__on_player_drops_item(player)`
### `__on_player_drops_stack(player)`
Triggered when the game receives the request from a player to drop one item or full stack from its inventory.
Event happens before anything is changed server side.

These events can be cancelled by returning `'cancel'`, which prevents the player dropping the items.

### `__on_player_picks_up_item(player, item)`
Triggered AFTER a player successfully ingested an item in its inventory. Item represents the total stack of items
ingested by the player. The exact position of these items is unknown as technically these
items could be spread all across the inventory.

### `__on_player_connects(player)`
Triggered when the player has successfully logged in and was placed in the game.

### `__on_player_disconnects(player, reason)`
Triggered when a player sends a disconnect package or is forcefully disconnected from the server.

### `__on_player_message(player, message)`
Triggered when a player sends a chat message.

### `__on_player_command(player, command)`
Triggered when a player runs a command. Command value is returned without the / in front.

This event can be cancelled by returning `'cancel'`, which prevents the message from being sent.

### `__on_statistic(player, category, event, value)`
Triggered when a player statistic changes. Doesn't notify on periodic an rhythmic events, i.e. 
`time_since_death`, `time_since_rest`, and `played_one_minute` since these are triggered every tick. Event 
is handled before scoreboard values for these statistics are changed.

## Custom events and hacking into scarpet event system

App programmers can define and trigger their own custom events. Unlike built-in events, all custom events pass a single value
as an argument, but this doesn't mean that they cannot pass a complex list, map, or nbt tag as a message. Each event signal is
either targetting all apps instances for all players, including global apps, if no target player has been identified, 
or only player scoped apps, if the target player
is specified, running once for that player app. You cannot target global apps with player-targeted signals. Built-in events
do target global apps, since their first argument is clearly defined and passed. That may change in the future in case there is 
a compelling argument to be able to target global apps with player scopes. 

Programmers can also handle built-in events the same way as custom events, as well as triggering built-in events, which I have
have no idea why you would need that. The following snippets have the same effect:

```
__on_player_breaks_block(player, block) -> print(player+' broke '+block);
```
and
```
handle_event('player_breaks_block', _(player, block) -> print(player+' broke '+block));
```

as well as
```
undef('__on_player_breaks_block');
```
and
```
handle_event('player_breaks_block', null);
```
And `signal_event` can be used as a trigger, called twice for player based built-in events
```
signal_event('player_breaks_block', player, player, block); // to target all player scoped apps
signal_event('player_breaks_block', null  , player, block); // to target all global scoped apps and all player instances
```
or (for global events)
```
signal_event('tick') // trigger all apps with a tick event
```

### `handle_event(event, callback ...)`

Provides a handler for an event identified by the '`event`' argument. If the event doesn't exist yet, it will be created.
All loaded apps globally can trigger that event, when they call corresponding `signal_event(event, ...)`. Callback can be
defined as a function name, function value (or a lambda function), along with optional extra arguments that will be passed
to it when the event is triggered. All custom events expect a function that takes one free argument, passed by the event trigger.
If extra arguments are provided, they will be appended to the argument list of the callback function.

Returns `true` if subscription to the event was successful, or `false` if it failed (for instance wrong scope for built-in event,
or incorect number of parameters for the event).

If a callback is specified as `null`, the given app (or player app instance )stops handling that event. 

<pre>
foo(a) -> print(a);
handle_event('boohoo', 'foo');

bar(a, b, c) -> print([a, b, c]);
handle_event('boohoo', 'bar', 2, 3) // using b = 2, c = 3, a - passed by the caller

handle_event('tick', _() -> foo('tick happened')); // built-in event

handle_event('tick', null)  // nah, ima good, kthxbai
</pre>

In case you want to pass an event handler that is not defined in your module, please read the tips on
 "Passing function references to other modules of your application" section in the `call(...)` section.


### `signal_event(event, target_player?, ... args?)`

Fires a specific event. If the event does not exist (only `handle_event` creates missing new events), or provided argument list
was not matching the callee expected arguments, returns `null`, 
otherwise returns number of apps notified. If `target_player` is specified and not `null` triggers a player specific event, targetting
only `player` scoped apps for that player. Apps with globals scope will not be notified even if they handle this event.
If the `target_player` is omitted or `null`, it will target `global` scoped apps and all instances of `player` scoped apps.
Note that all built-in player events have a player as a first argument, so to trigger these events, you need to 
provide them twice - once to specify the target player scope and second - to provide as an argument to the handler function.

<pre>
signal_event('player_breaks_block', player, player, block); // to target all player scoped apps
signal_event('player_breaks_block', null  , player, block); // to target all global scoped apps and all player instances
signal_event('tick') // trigger all apps with a tick event
</pre>

## Custom events example

The following example shows how you can communicate between different instances of the same player scoped app. It important to note
that signals can trigger other apps as well, assuming the name of the event matches. In this case the request name is called
`tp_request` and is triggered with a command.


``` 
// tpa.sc
global_requester = null;
__config() -> {
	'commands' -> {
		'<player>' -> _(to) -> signal_event('tp_request', to, player()),
      'accept' -> _() -> if(global_requester, 
         run('tp '+global_requester~'command_name'); 
         global_requester = null
      )
	},
   'arguments' -> {
      'player' -> {'type' -> 'players', 'single' -> true}
   }
};
handle_event('tp_request', _(req) -> (
   global_requester = req;
   print(player(), format(
      'w '+req+' requested to teleport to you. Click ',
      'yb here', '^yb here', '!/tpa accept',
      'w  to accept it.'
   ));
));
```

## `/script event` command

used to display current events and bounded functions. use `add_to` to register a new event, or `remove_from` to 
unbind a specific function from an event. Function to be bounded to an event needs to have the same number of 
parameters as the action is attempting to bind to (see list above). All calls in modules loaded via `/script load` 
that handle specific built-in events will be automatically bounded, and unbounded when script is unloaded.
