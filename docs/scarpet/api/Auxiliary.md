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

### `particle(name, pos, count?. spread?, speed?, player?)`

Renders a cloud of particles `name` centered around `pos` position, by default `count` 10 of them, default `speed` 
of 0, and to all players nearby, but these options can be changed via optional arguments. Follow vanilla `/particle` 
command on details on those options. Valid particle names are 
for example `'angry_villager', 'item diamond', 'block stone', 'dust 0.8 0.1 0.1 4'`.

### `particle_line(name, pos, pos2, density?, player?)`

Renders a line of particles from point `pos` to `pos2` with supplied density (defaults to 1), which indicates how far 
apart you would want particles to appear, so `0.1` means one every 10cm. If a player (or player name) is supplied, only
that player will receive particles.


### `particle_box(name, pos, pos2, density?, player?)`
### `particle_rect` (deprecated)

Renders a cuboid of particles between points `pos` and `pos2` with supplied density. If a player (or player name) is 
supplied, only that player will receive particles.

## Markers

### `draw_shape(shape, duration, key?, value?, ... )`, 
### `draw_shape(shape, duration, l(key?, value?, ... ))`, 
### `draw_shape(shape, duration, attribute_map)`
### `draw_shape(shape_list)`

Draws a shape in the world that will expire in `duration` ticks. Other attributes of the shape should be provided as 
consecutive key - value argument pairs, either as next arguments, or packed in a list, or supplied as a proper key-value
`map`. Arguments may include shared shape attributes, which are all optional, as well as shape-specific attributes, that
could be either optional or required. Shapes will draw properly on all carpet clients. Other connected players that don't
have carpet installed will still be able to see the required shapes in the form of dust particles. Replacement shapes
are not required to follow all attributes precisely, but will allow vanilla clients to receive some experience of your 
apps. One of the attributes that will definitely not be honored is the duration - particles will be send once
per shape and last whatever they typically last in the game.

Shapes can be send one by one, using either of the first three invocations, or batched as a list of shape descriptors. 
Batching has this benefit that they will be send possibly as one packet, limiting network overhead of 
sending many small packets to draw several shapes at once.

Shapes will fail to draw and raise a runtime error if not all its required parameters
are specified and all available shapes have some parameters that are required, so make sure to have them in place:

On the client, shapes can recognize that they are being redrawn again with the same parameters, disregarding the 
duration parameter. This updates the expiry on the drawn shape to the new value, instead of adding new shape in its 
place. This can be used for toggling the shapes on and off that has been send previously with very large durations, 
or simply refresh the shapes periodically in more dynamic applications.

Optional shared shape attributes:
 * `color` - integer value indicating the main color of the shape in the form of red, green, blue and alpha components 
 in the form of `0xRRGGBBAA`, with the default of `-1`, so white opaque, or `0xFFFFFFFF`.
 * `player` - name or player entity to send the shape to. If specified, the shapes will appear only for the specified
 player, otherwise it will be send to all players in the dimension.
 * `line` - line thickness, defaults to 2.0pt
 * `fill` - color for the faces, defaults to no fill. Use `color` attribute format
 * `follow` - entity, or player name. Shape will follow an entity instead of being static.
   Follow attribute requires all positional arguments to be relative to the entity and disallow
   of using entity or block as position markers. You must specify positions as a triple.
 * `snap` - if `follow` is present, indicated on which axis the snapping to entity coordinates occurs, and which axis
   will be treated statically, i.e. the coordinate passed in a coord triple is the actual value in the world. Default
   value is `'xyz'`, meaning the shape will be drawn relatively to the entity in all three directions. Using `xz` for 
   instance makes so that the shape follows the entity, but stays at the same, absolute Y coordinate.

Available shapes:
 * `'line'` - draws a straight line between two points
   * Required attributes:
     * `from` - triple coordinates, entity, or block value indicating one end of the line
     * `to` - other end of the line, same format as `from`
     
 * `'box'` - draws a box with corners in specified points
   * Required attributes:
     * `from` - triple coordinates, entity, or block value indicating one corner of the box
     * `to` - other corner, same format as `from`

 * `'sphere'`:
   * Required attributes:
     * `center` - center of the sphere
     * `radius` - radius of the sphere
   * Optional attributes:
     * `level` - level of details, or grid size. The more the denser your sphere. Default level of 0, means that the
      level of detail will be selected automatically based on radius.
      
 * `'cylinder'`:
   * Required attributes:
     * `center` - center of the base
     * `radius` - radius of the base circle
   * Optional attributes:
     * `axis` - cylinder direction, one of `'x'`, `'y'`, `'z'` defaults to `'y'`
     * `height` - height of the cyllinder, defaults to `0`, so flat disk. Can be negative.
     * `level` - level of details, see `'sphere'`.

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

### parse_nbt(tag)

Converts NBT tag to a scarpet value, which you can navigate through much better.

Converts:
 - Compound tags into maps with string keys
 - List tags into list values
 - Numbers (Ints, Floats, Doubles, Longs) into a number
 - Rest is converted to strings.
 
### encode_nbt(expr, force?)

Encodes value of the expression as an NBT tag. By default (or when `force` is false), it will only allow
to encode values that are guaranteed to return the same value when applied the resulting tag to `parse_nbt()`.
Supported types that can reliably convert back and forth to and from NBT values are:
 - Maps with string keywords
 - Lists of items of the same type (scarpet will take care of unifying value types if possible)
 - Numbers (encoded as Ints -> Longs -> Doubles, as needed)
 - Strings

Other value types will only be converted to tags (including NBT tags) if `force` is true. They would require
extra treatment when loading them back from NBT, but using `force` true will always produce output / never 
produce an exception.

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

### read_file(resource, type)
### delete_file(resource, type)
### write_file(resource, type, data, ...)

With the specified `resource` in the scripts folder, of a specific `type`, writes/appends `data` to it, reads its
 content, or deletes the resource.

Resource is identified by a path to the file.  
A path can contain letters, numbers, characters `-`, `+`, or `_`, and a folder separator: `'/'`. Any other characters are stripped
from the name. Empty descriptors are invalid. Do not add file extensions to the descriptor - extensions are inferred
based on the `type` of the file.
 
Resources can be located in the app specific space, or a shared space for all the apps. Accessing of app-specific
resources is guaranteed to be isolated from other apps. Shared resources are... well, shared across all apes, meaning
they can eat of each others file, however all access to files is synchronized, and files are never left open, so
this should not lead to any access problems.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, app
specific data directory is under `world/scripts/foo.data/...`, and shared data space is under
`world/scripts/shared/...`.

The default no-name app, via `/script run` command can only save/load/read files from the shared space.

Functions return `null` if an error is encounter or no file is present (for read and delete operations). Returns `true`
for success writes and deletes, and requested data, based on the file type, for read operations.

NBT files can be written once as they an store one tag at a time. Consecutive writes will overwrite previous data.

Write operations to text files always result in appending to the existing file, so consecutive writes will increase
the size of the file and add data to it. Since files are closed after each write, sending multiple lines of data to
write is beneficial for writing speed. To send multiple packs of data, either provide them flat or as a list in the
third argument.
 * `write_file('temp', 'text', 'foo', 'bar', 'baz')` or
 * write_file('temp', 'text', l('foo', 'bar', 'baz'))
 
To log a single line of string
 
Supported values for resource `type` is:
 * `nbt` - NBT tag
 * `text` - text resource with automatic newlines added
 * `raw` - text resource without implied newlines
 * `shared_nbt`, `shared_text`, `shared_raw` - shared versions of the above
 
NBT files have extension `.nbt`, store one NBT tag, and return a NBT type value. Text files have `.txt` extension, 
stores multiple lines of text and returns lists of all lines from the file. With `write_file`, multiple lines can be
sent to the file at once. The only difference between `raw` and `text` types are automatic newlines added after each
record to the file.

<pre>
write_file('foo', 'shared_text, l('one', 'two'));
write_file('foo', 'shared_text', 'three\n', 'four\n');
write_file('foo', 'shared_raw', 'five\n', 'six\n');

read_file('foo', 'shared_text')     => ['one', 'two', 'three', '', 'four', '', 'five', 'six']
</pre>
  
### `run(expr)`

Runs a vanilla command from the string result of the `expr` and returns its success count

<pre>
run('fill 1 1 1 10 10 10 air') -> 123 // 123 block were filled, this operation was successful 123 times out of a possible 1000 block volume
run('give @s stone 4') -> 1 // this operation was successful once
</pre>

### `save()`

Performs autosave, saves all chunks, player data, etc. Useful for programs where autosave is disabled due to 
performance reasons and saves the world only on demand.

### `load_app_data()`

NOTE: usages with arguments, so `load_app_data(file)` and `load_app_data(file, shared?)` are deprecated.
Use `read_file` instead.

Loads the app data associated with the app from the world /scripts folder. Without argument returns the memory 
managed and buffered / throttled NBT tag. With a file name, reads explicitly a file with that name from the 
scripts folder that belongs exclusively to the app. if `shared` is true, the file location is not exclusive
to the app anymore, but located in a shared app space. 

File descriptor can contain letters, numbers and folder separator: `'/'`. Any other characters are stripped
from the name before saving/loading. Empty descriptors are invalid. Do not add file extensions to the descriptor

Function returns nbt value with the file content, or `null` if the file is missing or there were problems
with retrieving the data.

The default no-name app, via `/script run` command can only save/load file from the shared data location.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, system-managed default app data storage is in `world/scripts/foo.data.nbt`, app
specific data directory is under `world/scripts/foo.data/bar/../baz.nbt`, and shared data space is under
`world/scripts/shared/bar/../baz.nbt`.

You can use app data to save non-vanilla information separately from the world and other scripts.

### `store_app_data(tag)`

Note:  `store_app_data(tag, file)` and `store_app_data(tag, file, shared?)` usages deprecated. Use `write_file` instead.

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
 
### `view_distance()`
Returns the view distance of the server.

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
*   `killed_by`: mobs killed by
*   `custom`: various random stats

For the options of `entry`, consult your statistics page, or give it a guess.

The call will return `null` if the statistics options are incorrect, or player doesn't have them in their history. 
If the player encountered the statistic, or game created for him empty one, it will return a number. 
Scarpet will not affect the entries of the statistics, even if it is just creating empty ones. With `null` response 
it could either mean your input is wrong, or statistic effectively has a value of `0`.
