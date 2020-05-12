# Auxiliary aspects

Collection of other methods that control smaller, yet still important aspects of the game

## Sounds

### `sound(name, pos, volume?, pitch?, mixer?)`

Plays a specific sound `name`, at block or position `pos`, with optional `volume` and modified `pitch`, and under
optional `mixer`. Default values for `volume`, `pitch` and `mixer` are `1.0`, `1.0`, and `master`. 
Valid mixer options are `master`, `music`, `record`, `weather`, `block`, `hostile`,`neutral`, `player`, `ambient`
and `voice`. `pos` can be either a block, triple of coords, or a list of thee numbers. Uses the same options as a
 corresponding `playsound` command.

## Particles

### `particle(name, pos, count?. spread?, speed?, playername?)`

Renders a cloud of particles `name` centered around `pos` position, by default `count` 10 of them, default `speed` 
of 0, and to all players nearby, but these options can be changed via optional arguments. Follow vanilla `/particle` 
command on details on those options. Valid particle names are 
for example `'angry_villager', 'item diamond', 'block stone', 'dust 0.8 0.1 0.1 4'`.

### `particle_line(name, pos, pos2, density?)`

Renders a line of particles from point `pos` to `pos2` with supplied density (defaults to 1), which indicates how far 
apart you would want particles to appear, so `0.1` means one every 10cm.

### `particle_rect(name, pos, pos2, density?)`

Renders a cuboid of particles between points `pos` and `pos2` with supplied density.

## Markers

### `create_marker(text, pos, rotation?, block?)`

Spawns a (permanent) marker entity with text or block at position. Returns that entity for further manipulations. 
Unloading the app that spawned them will cause all the markers from the loaded portion of the world to be removed. 
Also, if the game loads that marker in the future and the app is not loaded, it will be removed as well.

### `remove_all_markers()`

Removes all scarpet markers from the loaded portion of the world created by this app, in case you didn't want to do
 the proper cleanup.

## System function

### `nbt(expr)`

Treats the argument as a nbt serializable string and returns its nbt value. In case nbt is not in a correct nbt 
compound tag format, it will return `null` value.

Consult section about container operations in `Expression` to learn about possible operations on nbt values.

### `escape_nbt(expr)`

Excapes all the special characters in the string or nbt tag and returns a string that can be stored in nbt directly 
as a string value.

### `print(expr)`

Displays the result of the expression to the chat. Overrides default `scarpet` behaviour of sending everyting to stderr.

### `format(components, ...)`, `format(l(components, ...))`

Creates a line of formatted text. Each component is either a string indicating formatting and text it corresponds to
or a decorator affecting the component preceding it.

Regular formatting components is a string that have the structure of: 
`'<format> <text>'`, like `'gi Hi'`, which in this case indicates a grey, italicised word `'Hi'`. The space to separate the format and the text is mandatory. The format can be empty, but the space still
needs to be there otherwise the first word of the text will be used as format, which nobody wants.

Format is a list of formatting symbols indicating the format. They can be mixed and matched although color will only be
applied once. Available symbols include:
 * `i` - _italic_ 
 * `b` - **bold**
 * `s` - ~~strikethrough~~
 * `u` - <u>underline</u>
 * `o` - obfuscated

And colors:
 * `w` - White (default)
 * `y` - Yellow
 * `m` - Magenta (light purple)
 * `r` - Red
 * `c` - Cyan (aqua)
 * `l` - Lime
 * `t` - lighT blue
 * `f` - dark grayF (weird Flex, but ok)
 * `g` - Gray
 * `d` - golD
 * `p` - PurPle
 * `n` - browN (dark red)
 * `q` - turQuoise (dark aqua)
 * `e` - grEEn
 * `v` - naVy blue
 * `k` - blaK
 * `#FFAACC` - arbitrary RGB color (1.16+), hex notation. Use uppercase for A-F symbols
 
Decorators (listed as extra argument after the component they would affect):
 * `'^<format> <text>'` - hover over tooltip text, appearing when hovering with your mouse over the text below.
 * `'?<suggestion>` - command suggestion - a message that will be pasted to chat when text below it is clicked.
 * `'!<message>'` - a chat message that will be executed when the text below it is clicked.
 
Both suggestions and messages can contain a command, which will be executed as a player that clicks it.

So far the only usecase for formatted texts is with a `print` command. Otherwise it functions like a normal 
string value representing what is actually displayed on screen.
 
Example usages:
<pre>
 print(format('rbu Error: ', 'r Stuff happened!'))
 print(format('w Click ','tb [HERE]', '^di Awesome!', '!/kill', 'w \ button to win $1000'))
  // the reason why I backslash the second space is that otherwise command parser may contract consecutive spaces
  // not a problem in apps
</pre>

### `logger(expr)`

Prints the message to system logs, and not to chat.

### `run(expr)`

Runs a vanilla command from the string result of the `expr` and returns its success count

<pre>
run('fill 1 1 1 10 10 10 air') -> 123 // 123 block were filled, this operation was successful 123 times out of a possible 1000 block volume
run('give @s stone 4') -> 1 // this operation was successful once
</pre>

### `save()`

Performs autosave, saves all chunks, player data, etc. Useful for programs where autosave is disabled due to 
performance reasons and saves the world only on demand.

### `load_app_data(), load_app_data(file), load_app_data(file, shared?)`

Loads the app data associated with the app from the world /scripts folder. Without argument returns the memory 
managed and buffered / throttled NBT tag. With a file name, reads explicitly a file with that name from the 
scripts folder that belongs exclusively to the app. if `shared` is true, the file location is not exclusive
to the app anymore, but located in a shared app space. 

File descriptor can contain letters, numbers and folder separator: `'/'`. Any other characters are stripped
from the name before saving/loading. Empty descriptors are invalid.

Function returns nbt value with the file content, or `null` if the file is missing or there were problems
with retrieving the data.

The default no-name app, via `/script run` command can only save/load file from the shared data location.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, system-managed default app data storage is in `world/scripts/foo.data.nbt`, app
specific data directory is under `world/scripts/foo.data/bar/../baz.nbt`, and shared data space is under
`world/scripts/shared/bar/../baz.nbt`.

You can use app data to save non-vanilla information separately from the world and other scripts.

### `store_app_data(tag), store_app_data(tag, file), store_app_data(tag, file, shared?)`

Stores the app data associated with the app from the world `/scripts` folder. With the `file` parameter saves 
immediately and with every call to a specific file defined by the `file`, either in app space, or in the scripts
shared space if `shared` is true. Without `file` parameter, it may take up to 10
 seconds for the output file 
to sync preventing flickering in case this tag changes frequently. It will be synced when server closes.

Returns `true` if the file was saved successfully, `false` otherwise.

Uses the same file structure for exclusive app data, and shared data folder as `load_app_data`.

### `tick_time()`

Returns server tick counter. Can be used to run certain operations every n-th ticks, or to count in-game time.

### `world_time()`

Returns dimension-specific tick counter.

### `day_time(new_time?)`

Returns current daytime clock value. If `new_time` is specified, sets a new clock
to that value. Daytime clocks are shared between all dimensions.

### `last_tick_times()`

Returns a 100-long array of recent tick times, in milliseconds. First item on the list is the most recent tick
If called outside of the main tick (either throgh scheduled tasks, or async execution), then the first item on the
list may refer to the previous tick performance. In this case the last entry (tick 100) would refer to the most current
tick. For all intent and purpose, `last_tick_times():0` should be used as last tick execution time, but
individual tick times may vary greatly, and these need to be taken with the little grain of 
averaging.

### `game_tick(mstime?)`

Causes game to run for one tick. By default runs it and returns control to the program, but can optionally 
accept expected tick length, in milliseconds. You can't use it to permanently change the game speed, but setting 
longer commands with custom tick speeds can be interrupted via `/script stop` command

<pre>
loop(1000,game_tick())  // runs the game as fast as it can for 1000 ticks
loop(1000,game_tick(100)) // runs the game twice as slow for 1000 ticks
</pre>

### `seed()`

Returns current world seed.

### `current_dimension()`

Returns current dimension that the script runs in.

### `in_dimension(smth, expr)`

Evaluates the expression `expr` with different dimension execution context. `smth` can be an entity, 
world-localized block, so not `block('stone')`, or a string representing a dimension like:
 `'nether'`, `'the_nether'`, `'end'` or `'overworld'`, etc.

### `schedule(delay, function, args...)`

Schedules a user defined function to run with a specified `delay` ticks of delay. Scheduled functions run at the end 
of the tick, and they will run in order they were scheduled.

### `statistic(player, category, entry)`

Queries in-game statistics for certain values. Categories include:

*   `mined`: blocks mined
*   `crafted`: items crafted
*   `used`: items used
*   `broken`: items broken
*   `picked_up`: items picked up
*   `dropped`: items dropped
*   `killed`: mobs killed
*   `killed_by`: blocks mined
*   `custom`: various random stats

For the options of `entry`, consult your statistics page, or give it a guess.

The call will return `null` if the statistics options are incorrect, or player doesn't have them in their history. 
If the player encountered the statistic, or game created for him empty one, it will return a number. 
Scarpet will not affect the entries of the statistics, even if it is just creating empty ones. With `null` response 
it could either mean your input is wrong, or statistic effectively has a value of `0`.
