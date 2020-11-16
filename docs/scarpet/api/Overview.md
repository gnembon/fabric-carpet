# Minecraft specific API and `scarpet` language add-ons and commands

Here is the gist of the Minecraft related functions. Otherwise the CarpetScript could live without Minecraft.


## App structure

The main delivery method for scarpet programs into the game is in the form of apps in `*.sc` files located in the world `scripts` 
folder. When loaded (via `/script load` command, etc.), the game will run the content of the app once, regardless of its scope
(more about the app scopes below), without executing of any functions, unless called directly, and with the exception of the
`__config()` function, if present, which will be executed once. Loading the app will also bind specific 
events to the event system (check Events section for details).
 
Unloading an app removes all of its state from the game, disables commands, removes bounded events, and 
saves its global state. If more cleanup is needed, one can define `__on_close()` function which will be 
executed when the module is unloaded, or server is closing or crashing. However, there is no need to do that 
explicitly for the things that clean up automatically, as indicated in the previous statement. With `'global'` scoped
apps `__on_close()` will execute once per app, and with `'player'` scoped apps, will execute once per player per app. 

### App config via `__config()` function

If an app defines `__config` method, and that method returns a map, it will be used to apply custom settings 
for this app. Currently the following options are supported:

*   `'scope'`: default scope for global variables for the app, Default is `'player'`, which means that globals and defined 
functions will be unique for each player so that apps for each player will run in isolation. This is useful in 
tool-like applications, where behaviour of things is always from a player's perspective. With player scope the initial run 
of the app creates is initial state: defined functions, global variables, config and event handlers, which is then copied for 
each player that interacts with the app. With `'global'` scope - the state created by the initial load is the only variant of
the app state and all players interactions run in the same context, sharing defined functions, globals, config and events. 
`'global'` scope is most applicable to world-focused apps, where either players are not relevant, or player data is stored
explicitly keyed with players, player names, uuids, etc.
Even for `'player'` scoped apps, you can access specific player app with with commandblocks using
`/execute as <player> run script in <app> run ...`.
To access global/server state for a player app, you need to disown the command from any player, 
so either use a command block, or any 
arbitrary entity: `/execute as @e[type=bat,limit=1] run script in <app> globals` for instance, however
running anything in the global scope for a `'player'` scoped app is not recommended.
*   `'stay_loaded'`: defaults to `false`. If true, and `/carpet scriptsAutoload` is turned on, the following apps will 
stay loaded after startup. Otherwise, after reading the app the first time, and fetching the config, server will drop them down. 
This is to allow to store multiple apps on the server/world and selectively decide which one should be running at 
startup. WARNING: all apps will run once at startup anyways, so be aware that their actions that are called 
statically, will be performed once anyways.
*   `'legacy_command_type_support'` - if `true`, and the app defines the legacy command system via `__command()` function,
all parameters of command functions will be interpreted and used using brigadier / vanilla style argument parser and their type
will be inferred from their names, otherwise
the legacy scarpet variable parser will be used to provide arguments to commands.
*   `'allow_command_conflicts'` - if custom app commands tree is defined, the app engine will check and identify 
conflicts and ambiguities between different paths of execution. While ambiguous commands are allowed in brigadier,
and they tend to execute correctly, the suggestion support works really poorly in these situations and scarpet
will warn and prevent such apps from loading with an error message. If `allow_command_conflicts` is specified and 
`true`, then scarpet will load all provided commands regardless.
*   `'arguments'` - defines custom argument types for legacy commands with `'legacy_command_type_support'` as well
as for the custom commands defined with `'commands'`, see below
*   `'commands'` - defines custom commands for the app to be executed with `/<app>` command, see below

## Custom app commands

Apps can register custom commands added to the existing command system under `/<app>` where `<app>` is the 
name of the app. There are three ways apps can provide commands:

### Simple commands without custom argument support

Synopsis:
```
__command() -> 'root command'
foo() -> 'running foo';
bar(a, b) -> a + b;
baz(a, b) -> // same thing
(
    print(a+b);
    null
)
```

If a loaded app contains `__command()` method, it will attempt to register a command with that app name, 
and register all public (not starting with an underscore) functions available in the app as subcommands, in the form of
`/<app> <fun> <args...>`. Arguments are parsed from a single 
`greedy string` brigadier argument, and split into function parameters. Parsing of arguments is limited
to numbers, string constants, and available global variables, whitespace separated. Using functions and operators other than
unary `-`, would be unsafe, so it is not allowed.
In this mode, if a function returns a non-null value, it will be printed as a result to the 
invoker (e.g. in chat). If the provided argument list does not match the expected argument count of a function, an error message
will be generated.

Running the app command that doesn't take any extra arguments, so `/<app>` will run the `__command() -> ` function.

This mode is best for quick apps that typically don't require any arguments and want to expose some functionality in a 
simple and convenient way.

### Simple commands with vanilla argument type support

Synopsis:
 ```
__config() -> {'legacy_command_type_support' -> true};
__command() -> print('root command');
foo() -> print('running foo');
add(first_float, other_float) -> print('sum: '+(first_float+other_float)); 
bar(entitytype, item) -> print(entitytype+' likes '+item:0);
baz(entities) -> // same thing
 (
     print(join(',',entities));
 )
 ```

It works similarly to the auto command, but arguments get their inferred types based on the argument
names, looking at the full name, or the suffix after the last `_` that indicates the variable type. For instance, variable named `float` will
be parsed as a floating point number, but it can be named `'first_float'` or `'other_float'` as well. Any variable that is not
supported, will be parsed as a `'string'` type. 

Argument type support includes full support for custom argument types (see below).

### Custom commands

Synopsis. This example mimics vanilla `'effect'` command adding extra parameter that is not 
available in vanilla - to optionally hide effect icon from UI:
```
global_instant_effects = {'instant_health', 'instant_damage', 'saturation'};
__config() -> {
   'commands' -> 
   {
      '' -> _() -> print('this is a root call, does nothing. Just for show'),
      'clear' -> _() -> clear_all([player()]),
      'clear <entities>' -> 'clear_all',
      'clear <entities> <effect>' -> 'clear',
      'give <entities> <effect>' -> ['apply', -1, 0, false, true],
      'give <entities> <effect> <seconds>' -> ['apply', 0, false, true],
      'give <entities> <effect> <seconds> <amplifier>' -> ['apply', false, true],
      'give <entities> <effect> <seconds> <amplifier> <hideParticles>' -> ['apply', true],
      'give <entities> <effect> <seconds> <amplifier> <hideParticles> <showIcon>' -> 'apply',
      
   },
   'arguments' -> {
      'seconds' -> { 'type' -> 'int', 'min' -> 1, 'max' -> 1000000, 'suggest' -> [60]},
      'amplifier' -> { 'type' -> 'int', 'min' -> 0, 'max' -> 255, 'suggest' -> [0]},
      'hideParticles' -> {'type' -> 'bool'}, // pure rename
      'showIcon' -> {'type' -> 'bool'}, // pure rename
   }
};


clear_all(targets) -> for(targets, modify(_, 'effect'));
clear(targets, effect) -> for(targets, modify(_, 'effect', effect));
apply(targets, effect, seconds, amplifier, part, icon) -> 
(
   ticks = if (has(global_instant_effects, effect),  
      if (seconds < 0, 1, seconds),
      if (seconds < 0, 600, 20*seconds)
   );
   for (targets, modify(_, 'effect', effect, ticks, amplifier, part, icon));
)
```

This is the most flexible way to specify custom commands with scarpet. it works by providing command
paths with functions to execute, and optionally, custom argument types. Commands are listed in a map, where 
the key (can be empty) consists of 
the execution path with the command syntax, which consists of literals (as is) and arguments (wrapped with `<>`), with the name / suffix
of the name of the attribute indicating its type, and the value represent function to call, either function values,
defined function names, or functions with some default arguments. Values extracted from commands will be passed to the
functions and executed. By default, command list will be checked for ambiguities (commands with the same path up to some point
that further use different attributes), causing app loading error if that happens, however this can be suppressed by specifying
`'allow_command_conflicts'`.

Unlike with legacy command system with types support, names of the arguments and names of the function parameters don't need to match.
The only important aspect is the argument count and argument order.

Custom commands provide a substantial subset of brigadier features in a simple package, skipping purposely on some less common 
and less frequently used features, like forks and redirects, used pretty much only in the vanilla `execute` command.

### Command argument types

There are several default argument types that can be used directly without specifying custom types. Each argument can be 
customized in the `'arguments'` section of the app config, specifying its base type, via `'type'` that needs
to match any of the built-in types, with a series of optional modifiers. Shared modifiers include:
  * `suggest` - static list of suggestions to show above the command while typing
  * `suggester` - function taking one map argument, indicating current state of attributes in the parsed command
  suggesting a dynamic list of valid suggestions for typing. For instance here is a term based type matching 
  all loaded players adding Steve and Alex, and since player list changes over time cannot be provided statically:
  ```
__config() -> {
   'arguments' -> {
      'loadedplayer' -> {
         'type' -> 'term',
         'suggester' -> _(args) -> (
            nameset = {'Steve', 'Alex'}; 
            for(player('all'), nameset += _);
            keys(nameset)
         ),
      }
   }
};
  ```
  * `case_sensitive` - whether suggestions are case sensitive, defaults to true
  
Here is a list of built-in types, with their return value formats, as well as a list of modifiers
 that can be customized for that type (if any)
  * `'string'`: a string that can be quoted to include spaces. Customizable with `'options'` - a 
  static list of valid options it can take. command will fail if the typed string is not in this list.
  * `'term'`: single word string, no spaces. Can also be customized with `'options'`
  * `'text'`: the rest of the command as a string. Has to be the last argument. Can also be customized with `'options'`
  * `'bool'`: `true` or `false`
  * `'float'`: a number. Customizable with `'min'` and `'max'` values.
  * `'int'`: a number, requiring an integer value. Customizable with `'min'` and `'max'` values.
  * `'yaw'`: a number, requiring a valid yaw angle.
  * `'pos'`: block position as a triple of coordinates. Customized with `'loaded'`, if true requiring the position 
  to be loaded.
  * `'block'`: a valid block state wrapped in a block value (including block properties and data)
  * `'teamcolor'`: name of a team, and an integer color value of one of 16 valid team colors.
  * `'columnpos'`: a pair of x and z coordinates.
  * `'dimension'`: string representing a valid dimension in the world.
  * `'anchor'`: string of `feet` or `eyes`.
  * `'entitytype'`: string representing a type of entity 
  * `'floatrange'`: pair of two numbers where one is smaller than the other 
  * `'players'`: returning a list of valid player name string, logged in or not. If configured with `'single'` returns only one player or `null`.
  * `'intrange'`: same as `'floatrange'`, but requiring integers. 
  * `'enchantment'`: name of an enchantment
  * `'item'`: triple of item type, count of 1, and nbt.
  * `'message'`: text with expanded embedded player names 
  * `'effect'`: string representing a status effect
  * `'path'`: a valid nbt path 
  * `'objective'`: a tuple of scoreboard objective name and its criterion
  * `'criterion'`: name of a scoreboard criterion 
  * `'particle'`: name of a particle 
  * `'recipe'`: name of a valid recipe. Can be fed to recipe_data function.
  * `'advancement'`: name of an advancement 
  * `'lootcondition'`: a loot condition 
  * `'loottable'`: name of a loot table source
  * `'attribute'`: an attribute name
  * `'boss'`: a bossbar name
  * `'biome'`: a biome name 
  * `'sound'`: name of a sound 
  * `'storekey'`: string of a valid current data store key.
  * `'identifier'`: any valid identifier. 'minecraft:' prefix is stripped off as a default.
  Configurable with `'options'` parameter providing a static list of valid identifiers.
  * `'rotation'`: pair of two numbers indicating yaw and pitch values.
  * `'scoreholder'`: list of strings of valid score holders. Customizable with `'single'` that makes the parameter require a single target, retuning `null` if its missing
  * `'scoreboardslot'` string representing a valid location of scoreboard display.
  * `'swizzle'` - set of axis as a string, sorted.
  * `'time'` - number of ticks representing a duration of time.
  * `'uuid'` - string of a valid uuid.
  * `'surfacelocation'` - pair of x and z coordinates, floating point numbers.
  * `'location'` - triple of x, y and z coordinates, optionally centered on the block if 
  interger coordinates are provided and `'block_centered'` optional modifier is `true`.   

## Dimension warning

One note, which is important is that most of the calls for entities and blocks would refer to the current 
dimension of the caller, meaning, that if we for example list all the players using `player('all')` function, 
if a player is in the other dimension, calls to entities and blocks around that player would be incorrect. 
Moreover, running commandblocks in the spawn chunks would mean that commands will always refer to the overworld 
blocks and entities. In case you would want to run commands across all dimensions, just run three of them, 
using `/execute in overworld/the_nether/the_end run script run ...` and query players using `player('*')`, 
which only returns players in current dimension, or use `in_dimension(expr)` function.