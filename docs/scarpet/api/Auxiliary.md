# Auxiliary aspects

Collection of other methods that control smaller, yet still important aspects of the game

## Sounds

### `sound()`, `sound(name, pos, volume?, pitch?, mixer?)`

Plays a specific sound `name`, at block or position `pos`, with optional `volume` and modified `pitch`, and under
optional `mixer`. Default values for `volume`, `pitch` and `mixer` are `1.0`, `1.0`, and `master`. 
Valid mixer options are `master`, `music`, `record`, `weather`, `block`, `hostile`,`neutral`, `player`, `ambient`
and `voice`. `pos` can be either a block, triple of coords, or a list of thee numbers. Uses the same options as a
 corresponding `playsound` command.
 
Used with no arguments, return the list of available sound names.
 
Throws `unknown_sound` if sound doesn't exist.

## Particles

### `particle()`, `particle(name, pos, count?. spread?, speed?, player?)`

Renders a cloud of particles `name` centered around `pos` position, by default `count` 10 of them, default `speed` 
of 0, and to all players nearby, but these options can be changed via optional arguments. Follow vanilla `/particle` 
command on details on those options. Valid particle names are 
for example `'angry_villager', 'item diamond', 'block stone', 'dust 0.8 0.1 0.1 4'`.

Used with no arguments, return the list of available particle names. Note that some of the names do not correspond to a valid
particle that can be fed to `particle(...)` function due to a fact that some particles need more configuration
to be valid, like `dust`, `block` etc. Should be used as a reference only.

Throws `unknown_particle` if particle doesn't exist.

### `particle_line(name, pos, pos2, density?, player?)`

Renders a line of particles from point `pos` to `pos2` with supplied density (defaults to 1), which indicates how far 
apart you would want particles to appear, so `0.1` means one every 10cm. If a player (or player name) is supplied, only
that player will receive particles.

Throws `unknown_particle` if particle doesn't exist.

### `particle_box(name, pos, pos2, density?, player?)`
### `particle_rect` (deprecated)

Renders a cuboid of particles between points `pos` and `pos2` with supplied density. If a player (or player name) is 
supplied, only that player will receive particles.

Throws `unknown_particle` if particle doesn't exist.

## Markers

### `draw_shape(shape, duration, key?, value?, ... )`, 
### `draw_shape(shape, duration, [key?, value?, ... ])`, 
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
sending many small packets to draw several shapes at once. The drawback of sending shapes is batches is that they need to address
the same list of players, i.e. if multiple players from the list target different players, all shapes will be sent to all of them.

Shapes will fail to draw and raise a runtime error if not all its required parameters
are specified and all available shapes have some parameters that are required, so make sure to have them in place:

On the client, shapes can recognize that they are being redrawn again with the same parameters, disregarding the 
duration parameter. This updates the expiry on the drawn shape to the new value, instead of adding new shape in its 
place. This can be used for toggling the shapes on and off that has been send previously with very large durations, 
or simply refresh the shapes periodically in more dynamic applications.

Optional shared shape attributes:
 * `color` - integer value indicating the main color of the shape in the form of red, green, blue and alpha components 
 in the form of `0xRRGGBBAA`, with the default of `-1`, so white opaque, or `0xFFFFFFFF`.
 * `player` - name or player entity to send the shape to, or a list of players. If specified, the shapes will appear only for the specified
 players (regardless where they are), otherwise it will be send to all players in the current dimension.
 * `line` - (Deprecated) line thickness, defaults to 2.0pt. Not supported in 1.17's 3.2 core GL renderer.
 * `fill` - color for the faces, defaults to no fill. Use `color` attribute format
 * `follow` - entity, or player name. Shape will follow an entity instead of being static.
   Follow attribute requires all positional arguments to be relative to the entity and disallow
   of using entity or block as position markers. You must specify positions as a triple.
 * `snap` - if `follow` is present, indicated on which axis the snapping to entity coordinates occurs, and which axis
   will be treated statically, i.e. the coordinate passed in a coord triple is the actual value in the world. Default
   value is `'xyz'`, meaning the shape will be drawn relatively to the entity in all three directions. Using `xz` for 
   instance makes so that the shape follows the entity, but stays at the same, absolute Y coordinate. Preceeding an axis
   with `d`, like `dxdydz` would make so that entity position is treated discretely (rounded down).
 * `debug` - if True, it will only be visible when F3+B entity bounding boxes is enabled.
 * `facing` - applicable only to `'text'`, `'block'` or '`item'` shapes, where its facing. Possible options are:
   * `player`: Default. Element always rotates to face the player eye position, 
   * `camera`: Element is placed on the plane orthogonal to player look vector, 
   * `north`, `south`, `east`, `west`, `up`, `down`: obvious

Available shapes:
 * `'line'` - draws a straight line between two points.
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

 * `'polygon'`:
   * Required attributes:
     * `points` - list of points defining vertices of the polygon
   * Optional attributes:
     * `relative` - list of bools. vertices of the polygon that affected by 'follow'. Could be a single bools to affact allpoints too. Default means that every point is affacted.
     * `mode` - how those points are connected. may be "polygon"(default),"strip" or "triangles". "polygon" means that it will be viewed as vertices of a polygon center on the first one. "strip" means that it will be viewed as a triangles strip. "triangles" means that it will be viewed as some triangles that are not related to each other (therefor length of `points` in this mode have to be a multiple of 3).
     * `inner` - if `true` it will make the inner edges be drawn as well. 
     * `doublesided` - if `true` it will make the shapes visible from the back as well. Default is `true`. 

 * `'label'` - draws a text in the world. Default `line` attribute controls main font color. 
 `fill` controls the color of the background.
   * Required attributes:
     * `pos` - position
     * `text` - string or formatted text to display
   * Optional attributes
     * `value` - string or formatted text to display instead of the main `text`. `value` unlike `text`
         is not used to determine uniqueness of the drawn text so can be used to
         display smoothly dynamic elements where value of an element is constantly
         changing and updates to it are being sent from the server.
     * `size` - float. Default font size is 10.
     * `doublesided` - if `true` it will make the text visible from the back as well. Default is `false` (1.16+)
     * `align` - text alignment with regards to `pos`. Default is `center` (displayed text is
         centered with respect to `pos`), `left` (`pos` indicates beginning of text), and `right` (`pos`
         indicates the end of text).
     * `tilt`, `lean`, `turn` - additional rotations of the text on the canvas along all three axis
     * `indent`, `height`, `raise` - offsets for text rendering on X (`indent`), Y (`height`), and Z axis (`raise`)
         with regards to the plane of the text. One unit of these corresponds to 1 line spacing, which
         can be used to display multiple lines of text bound to the same `pos`

 * `'block'`: draws a block at the specified position:
   * Required attributes:
     * `pos` - position of the object.
     * `block` - the object to show. It is a block value or a name of a block with optional NBT data.
   * Optional attributes:
     * `tilt`, `lean`, `turn` - additional rotations along all three axis. It uses the block center as the origin.
     * `scale` - scale of it in 3 axis-direction. should be a number or a list of 3 numbers (x,y,z).
     * `skylight`, `blocklight` - light level. omit it to use local light level. should between 0~15.

 * `'item'`: draws an item at the specified position:
   * Required attributes:
     * `pos` - position of the object.
     * `item` - the object to show. It is an item tuple or a string identified item that may have NBT data.
   * Optional attributes:
     * `tilt`, `lean`, `turn` - additional rotations along all three axis. for `block`, it use its block center as the origin.
     * `scale` - scale of it in 3 axis-direction. should be a number or a list of 3 numbers (x,y,z).
     * `skylight`, `blocklight` - light level. omit it to use local light level. should between 0~15.
     * `variant` - one of `'none'`, `'third_person_left_hand'`, `'third_person_right_hand'`, `'first_person_left_hand'`,
       `'first_person_right_hand'`, `'head'`, `'gui'`, `'ground'`, `'fixed'`. In addition to the literal meaning,
       it can also be used to use special models of tridents and telescopes. 
        This attribute is experimental and use of it will change in the future.

      
### `create_marker(text, pos, rotation?, block?, interactive?)`

Spawns a (permanent) marker entity with text or block at position. Returns that entity for further manipulations. 
Unloading the app that spawned them will cause all the markers from the loaded portion of the world to be removed. 
Also, if the game loads that marker in the future and the app is not loaded, it will be removed as well. 

If `interactive` (`true` by default) is `false`, the armorstand will be a marker and would not be interactive in any
gamemode. But blocks can be placed inside markers and will not catch any interaction events. 

Y Position of a marker text or block will be adjusted to make blocks or text appear at the specified position. 
This makes so that actual armorstand position may be offset on Y axis. You would need to adjust your entity
locations if you plan to move the armorstand around after the fact. If both text and block are specified - one of them
will be aligned (armorstand type markers text shows up at their feet, while for regular armorstands - above the head,
while block on the head always render in the same position regardless if its a marker or not).


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

### `tag_matches(daddy_tag, baby_tag, match_lists?)`

Utility returning `true` if `baby_tag` is fully contained in `daddy_tag`. Anything matches `null` baby tag, and
Nothing is contained in a `null` daddy tag. If `match_lists` is specified and `false`, content of nested lists is ignored. 
Default behaviour is to match them.

### `parse_nbt(tag)`

Converts NBT tag to a scarpet value, which you can navigate through much better.

Converts:
 - Compound tags into maps with string keys
 - List tags into list values
 - Numbers (Ints, Floats, Doubles, Longs) into a number
 - Rest is converted to strings.
 
### `encode_nbt(expr, force?)`

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

### `print(expr)`, `print(player/player_list, expr)`

Displays the result of the expression to the chat. Overrides default `scarpet` behaviour of sending everyting to stderr.
Can optionally define player or list of players to send the message to.

### `format(components, ...)`, `format([components, ...])`

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
 * `'@<url>'` - a URL that will be opened when the text below it is clicked.
 * `'&<text>'` - a text that will be copied to clipboard when the text below it is clicked.
 
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

### `item_display_name(item)`
 Returns the name of the item as a Text Value. `item` should be a list of `[item_name, count, nbt]`, or just an item name.
 
 Please note that it is a translated value. treating it like a string (eg.slicing, breaking, changing its case) will turn it back into a normal string without translatable properties. just like a colorful formatted text loose its color. And the result of it converting to a string will use en-us (in a server) or your single player's language, but when you use print() or others functions that accept a text value to broadcast it to players, it will use each player's own language.
 
 If the item is renamed, it will also be reflected in the results.


### `display_title(players, type, text?, fadeInTicks?, stayTicks?, fadeOutTicks),`

Sends the player (or players if `players` is a list) a title of a specific type, with optionally some times.
 * `players` is either an online player or a list of players. When sending a single player, it will throw if the player is invalid or offline.
 * `type` is either `'title'`, `'subtitle'`, `actionbar` or `clear`.
   Note: `subtitle` will only be displayed if there is a title being displayed (can be an empty one)
 * `title` is what title to send to the player. It is required except for `clear` type. Can be a text formatted using `format()`
 * `...Ticks` are the number of ticks the title will stay in that state.
   If not specified, it will use current defaults (those defaults may have changed from a previous `/title times` execution).
   Executing with those will set the times to the specified ones.
   Note that `actionbar` type doesn't support changing times (vanilla bug, see [MC-106167](https://bugs.mojang.com/browse/MC-106167)).

### `display_title(players, 'player_list_header', text)`
### `display_title(players, 'player_list_footer', text)`

Changes the header or footer of the player list for the specified targets.
If `text` is `null` or an empty string it will remove the header or footer for the specified targets.
In case the player has Carpet loggers running, the footer specified by Scarpet will appear above the loggers.

### `logger(msg), logger(type, msg)`

Prints the message to system logs, and not to chat.
By default prints an info, unless you specify otherwise in the `type` parameter.

Available output types:

`'debug'`, `'warn'`, `'fatal'`, `'info'` and `'error'`


### `read_file(resource, type)`
### `delete_file(resource, type)`
### `write_file(resource, type, data, ...)`
### `list_files(resource, type)`

With the specified `resource` in the scripts folder, of a specific `type`, writes/appends `data` to it, reads its
 content, deletes the resource, or lists other files under this resource.

Resource is identified by a path to the file.  
A path can contain letters, numbers, characters `-`, `+`, or `_`, and a folder separator: `'/'`. Any other characters are stripped
from the name. Empty descriptors are invalid, except for `list_files` where it means the root folder.
 Do not add file extensions to the descriptor - extensions are inferred
based on the `type` of the file. A path can have one `'.zip'` component indicating a zip folder allowing to read / write to and from
zip files, although you cannot nest zip files in other zip files. 
 
Resources can be located in the app specific space, or a shared space for all the apps. Accessing of app-specific
resources is guaranteed to be isolated from other apps. Shared resources are... well, shared across all apes, meaning
they can eat of each others file, however all access to files is synchronized, and files are never left open, so
this should not lead to any access problems.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, app
specific data directory is under `world/scripts/foo.data/...`, and shared data space is under
`world/scripts/shared/...`.

The default no-name app, via `/script run` command can only save/load/read files from the shared space.

Functions return `null` if no file is present (for read, list and delete operations). Returns `true`
for success writes and deletes, and requested data, based on the file type, for read operations. It returns list of files 
for folder listing.
 
Supported values for resource `type` are:
 * `nbt` - NBT tag
 * `json` - JSON file
 * `text` - text resource with automatic newlines added
 * `raw` - text resource without implied newlines
 * `folder` - for `list_files` only - indicating folder listing instead of files
 * `shared_nbt`, `shared_text`, `shared_raw`, `shared_folder`, `shared_json` - shared versions of the above
 
NBT files have extension `.nbt`, store one NBT tag, and return a NBT type value. JSON files have `.json` extension, store 
Scarpet numbers, strings, lists, maps and `null` values. Anything else will be saved as a string (including NBT).  
Text files have `.txt` extension, 
stores multiple lines of text and returns lists of all lines from the file. With `write_file`, multiple lines can be
sent to the file at once. The only difference between `raw` and `text` types are automatic newlines added after each
record to the file. Since files are closed after each write, sending multiple lines of data to
write is beneficial for writing speed. To send multiple packs of data, either provide them flat or as a list in the
third argument.

Throws:
- `nbt_read_error`: When failed to read NBT file.
- `json_read_error`: When failed to read JSON file. The exception data will contain details about the problem.
- `io_exception`: For all other errors when handling data on disk not related to encoding issues

All other errors resulting of improper use of input arguments should result in `null` returned from the function, rather than exception
thrown.

<pre>
write_file('foo', 'shared_text, ['one', 'two']);
write_file('foo', 'shared_text', 'three\n', 'four\n');
write_file('foo', 'shared_raw', 'five\n', 'six\n');

read_file('foo', 'shared_text')     => ['one', 'two', 'three', '', 'four', '', 'five', 'six']
</pre>
  
### `run(expr)`

Runs a vanilla command from the string result of the `expr` and returns a triple of success count, 
intercepted list of output messages, and error message if the command resulted in a failure. 
Successful commands return `null` as their error.

<pre>
run('fill 1 1 1 10 10 10 air') -> [123, ["Successfully filled 123 blocks"], null] // 123 block were filled, this operation was successful 123 times out of a possible 1000 block volume
run('give @s stone 4') -> [1, ["Gave 4 [Stone] to gnembon"], null] // this operation was successful once
run('seed') -> [-170661413, ["Seed: [4031384495743822299]"], null]
run('sed') -> [0, [], "sed<--[HERE]"] // wrong command
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

Throws `nbt_read_error` if failed to read app data.

### `store_app_data(tag)`

Note:  `store_app_data(tag, file)` and `store_app_data(tag, file, shared?)` usages deprecated. Use `write_file` instead.

Stores the app data associated with the app from the world `/scripts` folder. With the `file` parameter saves 
immediately and with every call to a specific file defined by the `file`, either in app space, or in the scripts
shared space if `shared` is true. Without `file` parameter, it may take up to 10
 seconds for the output file 
to sync preventing flickering in case this tag changes frequently. It will be synced when server closes.

Returns `true` if the file was saved successfully, `false` otherwise.

Uses the same file structure for exclusive app data, and shared data folder as `load_app_data`.

### `create_datapack(name, data)`

Creates and loads custom datapack. The data has to be a map representing the file structure and the content of the 
json files of the target pack.

Returns `null` if the pack with this name already exists or is loaded, meaning no change has been made.
Returns `false` if adding of the datapack wasn't successful.
Returns `true` if creation and loading of the datapack was successful. Loading of a datapack results in
reloading of all other datapacks (vanilla restrictions, identical to /datapack enable), however unlike with `/reload` 
command, scarpet apps will not be reloaded by adding a datapack using `create_datapack`.

Currently, only json/nbt/mcfunction files are supported in the packs. `'pack.mcmeta'` file is added automatically.

Reloading of datapacks that define new dimensions is not implemented in vanilla. Vanilla game only loads 
dimension information on server start. `create_datapack` is therefore a direct replacement of manually ploping of the specified 
file structure in a datapack file and calling `/datapack enable` on the new datapack with all its quirks and sideeffects
(like no worldgen changes, reloading all other datapacks, etc.). To enable newly added custom dimensions, call much more
experimental `enable_hidden_dimensions()` after adding a datapack if needed.

Synopsis:
<pre>
script run create_datapack('foo', 
{
    'foo' -> { 'bar.json' -> {
        'c' -> true,
        'd' -> false,
        'e' -> {'foo' -> [1,2,3]},
        'a' -> 'foobar',
        'b' -> 5
    } }
})
</pre>

Custom dimension example:
<pre>
// 1.17
script run create_datapack('funky_world',  {
    'data' -> { 'minecraft' -> { 'dimension' -> { 'custom_ow.json' -> { 
        'type' -> 'minecraft:the_end',
        'generator' -> {
            'biome_source' -> {
                 'seed' -> 0,
                 'large_biomes' -> false,
                 'type' -> 'minecraft:vanilla_layered'
            },
            'seed' -> 0,
            'settings' -> 'minecraft:nether',
            'type' -> 'minecraft:noise'
    } } } } }
});

// 1.18
script run a() -> create_datapack('funky_world',  {
   'data' -> { 'minecraft' -> { 'dimension' -> { 'custom_ow.json' -> { 
      'type' -> 'minecraft:overworld',
         'generator' -> {
            'biome_source' -> {
               'biomes' -> [
                  {
                     'parameters' -> {                        
                        'erosion' -> [-1.0,1.0], 
                        'depth' -> 0.0, 
                        'weirdness' -> [-1.0,1.0],
                        'offset' -> 0.0,
                        'temperature' -> [-1.0,1.0],
                        'humidity' -> [-1.0,1.0],
                        'continentalness' -> [ -1.2,-1.05]
                     },
                     'biome' -> 'minecraft:mushroom_fields'
                  }
               ],
               'type' -> 'minecraft:multi_noise'
            },
            'seed' -> 0,
            'settings' -> 'minecraft:overworld',
            'type' -> 'minecraft:noise'
         }
     } } } }
});
enable_hidden_dimensions();  => ['funky_world']
</pre>

Loot table example:
<pre>
script run create_datapack('silverfishes_drop_gravel', {
    'data' -> { 'minecraft' -> { 'loot_tables' -> { 'entities' -> { 'silverfish.json' -> {
        'type' -> 'minecraft:entity',
        'pools' -> [
            {
                'rolls' -> {
                    'min' -> 0,
                    'max' -> 1
                },
                'entries' -> [
                    {
                        'type' -> 'minecraft:item',
                        'name' -> 'minecraft:gravel'
                    }
                ]
            }
        ]
    } } } } }
});
</pre>

Recipe example:
<pre>
script run create_datapack('craftable_cobwebs', {
    'data' -> { 'scarpet' -> { 'recipes' -> { 'cobweb.json' -> {
        'type' -> 'crafting_shaped',
        'pattern' -> [
            'SSS',
            'SSS',
            'SSS'
        ],
        'key' -> {
            'S' -> {
                'item' -> 'minecraft:string'
            }
        },
        'result' -> {
            'item' -> 'minecraft:cobweb',
            'count' -> 1
        }
    } } } }
});
</pre>

Function example:
<pre>
 script run create_datapack('example',{'data/test/functions/talk.mcfunction'->'say 1\nsay 2'})
</pre>
### `enable_hidden_dimensions()` (1.18.1 and lower)

The function reads current datapack settings detecting new dimensions defined by these datapacks that have not yet been added
to the list of current dimensions and adds them so that they can be used and accessed right away. It doesn't matter how the
datapacks have been added to the game, either with `create_datapack()` or manually by dropping a datapack file and calling 
`/datapack enable` on it. Returns the list of valid dimension names / identifiers that has been added in the process.

Fine print: The function should be
considered experimental. For example: is not supposed to work at all in vanilla, and its doing exactly that in 1.18.2+.
There 'should not be' (famous last words) any side-effects if no worlds are added. Already connected
clients will not see suggestions for commands that use dimensions `/execute in <dim>` (vanilla client limitation) 
but all commands should work just fine with
the new dimensions. Existing worlds that have gotten modified settings by the datapacks will not be reloaded or replaced.
The usability of the dimensions added this way has not been fully tested, but it seems it works just fine. Generator settings
for the new dimensions will not be added to `'level.dat'` but it will be added there automatically next time the game restarts by 
vanilla. One could have said to use this method with caution, and the authors take no responsibility of any losses incurred due to 
mis-handlilng of the temporary added dimensions, yet the feature itself (custom dimensions) is clearly experimental for Mojang 
themselves, so that's about it.

### `tick_time()`

Returns server tick counter. Can be used to run certain operations every n-th ticks, or to count in-game time.

### `world_time()`

_**Deprecated**. Use `system_info('world_time')` instead._

Returns dimension-specific tick counter.

### `day_time(new_time?)`

Returns current daytime clock value. If `new_time` is specified, sets a new clock
to that value. Daytime clocks are shared between all dimensions.

### `last_tick_times()`

_**Deprecated**. Use `system_info('server_last_tick_times')` instead._

Returns a 100-long array of recent tick times, in milliseconds. First item on the list is the most recent tick
If called outside of the main tick (either throgh scheduled tasks, or async execution), then the first item on the
list may refer to the previous tick performance. In this case the last entry (tick 100) would refer to the most current
tick. For all intent and purpose, `last_tick_times():0` should be used as last tick execution time, but
individual tick times may vary greatly, and these need to be taken with the little grain of 
averaging.

### `game_tick(mstime?)`

Causes game to run for one tick. By default, it runs it and returns control to the program, but can optionally 
accept expected tick length, in milliseconds, waits that extra remaining time and then returns the control to the program.
You can't use it to permanently change the game speed, but setting 
longer commands with custom tick speeds can be interrupted via `/script stop` command - if you can get access to the 
command terminal.

Running `game_tick()` as part of the code that runs within the game tick itself is generally a bad idea, 
unless you know what this entails. Triggering the `game_tick()` will cause the current (shoulder) tick to pause, then run the internal tick, 
then run the rest of the shoulder tick, which may lead to artifacts in between regular code execution and your game simulation code.
If you need to break
up your execution into chunks, you could queue the rest of the work into the next task using `schedule`, or perform your actions
defining `__on_tick()` event handler, but in case you need to take a full control over the game loop and run some simulations using 
`game_tick()` as the way to advance the game progress, that might be the simplest way to do it, 
and triggering the script in a 'proper' way (there is not 'proper' way, but via commmand line, or server chat is the most 'proper'),
would be the safest way to do it. For instance, running `game_tick()` from a command block triggered with a button, or in an entity
 event triggered in an entity tick, may technically
cause the game to run and encounter that call again, causing stack to overflow. Thankfully it doesn't happen in vanilla running 
carpet, but may happen with other modified (modded) versions of the game.

<pre>
loop(1000,game_tick())  // runs the game as fast as it can for 1000 ticks
loop(1000,game_tick(100)) // runs the game twice as slow for 1000 ticks
</pre>


### `seed()` deprecated

Returns current world seed. Function is deprecated, use `system_info('world_seed')` insteads.

### `current_dimension()`

Returns current dimension that the script runs in.

### `in_dimension(smth, expr)`

Evaluates the expression `expr` with different dimension execution context. `smth` can be an entity, 
world-localized block, so not `block('stone')`, or a string representing a dimension like:
 `'nether'`, `'the_nether'`, `'end'` or `'overworld'`, etc.
 
Throws `unknown_dimension` if provided dimension can't be found.
 
### `view_distance()`

_**Deprecated**. Use `system_info('game_view_distance')` instead._

Returns the view distance of the server.

### `get_mob_counts()`, `get_mob_counts(category)` 1.16+

Returns either a map of mob categories with its respective counts and capacities (a.k.a. mobcaps) or just a tuple
of count and limit for a specific category. If a category was not spawning for whatever reason it may not be
returned from `get_mob_counts()`, but could be retrieved for `get_mob_counts(category)`. Returned counts is what spawning
algorithm has taken in to account last time mobs spawned. 

### `schedule(delay, function, args...)`

Schedules a user defined function to run with a specified `delay` ticks of delay. Scheduled functions run at the end 
of the tick, and they will run in order they were scheduled.

In case you want to schedule a function that is not defined in your module, please read the tips on
 "Passing function references to other modules of your application" section in the `call(...)` section.

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


### `system_info()`, `system_info(property)`
Fetches the value of one of the following system properties. If called without arguments, it returns a list of 
available system_info options. It can be used to 
fetch various information, mostly not changing, or only available via low level
system calls. In all circumstances, these are only provided as read-only.

##### Available options in the scarpet app space:
  * `app_name` - current app name or `null` if its a default app
  * `app_list` - list of all loaded apps excluding default commandline app
  * `app_scope` - scope of the global variables and function. Available options is `player` and `global`
  * `app_players` - returns a player list that have app run under them. For `global` apps, the list is always empty
 
##### Relevant world related properties
  * `world_name` - name of the world
  * `world_seed` - a numeric seed of the world
  * `world_dimensions` - a list of dimensions in the world
  * `world_path` - full path to the world saves folder
  * `world_folder` - name of the direct folder in the saves that holds world files
  * `world_carpet_rules` - returns all Carpet rules in a map form (`rule`->`value`). Note that the values are always returned as strings, so you can't do boolean comparisons directly. Includes rules from extensions with their namespace (`namespace:rule`->`value`). You can later listen to rule changes with the `on_carpet_rule_changes(rule, newValue)` event.
  * `world_gamerules` - returns all gamerules in a map form (`rule`->`value`). Like carpet rules, values are returned as strings, so you can use appropriate value conversions using `bool()` or `number()` to convert them to other values. Gamerules are read-only to discourage app programmers to mess up with the settings intentionally applied by server admins. Isn't that just super annoying when a datapack messes up with your gamerule settings? It is still possible to change them though using `run('gamerule ...`.
  * `world_spawn_point` - world spawn point in the overworld dimension
  * `world_time` - Returns dimension-specific tick counter.
  * `world_top` - Returns current dimensions' topmost Y value where one can place blocks.
  * `world_bottom` - Returns current dimensions' bottommost Y value where one can place blocks.
  * `world_center` - Returns coordinates of the center of the world with respect of the world border
  * `world_size` - Returns radius of world border for current dimension.
  * `world_max_size` - Returns maximum possible radius of world border for current dimension.
  * 
##### Relevant gameplay related properties
  * `game_difficulty` - current difficulty of the game: `'peaceful'`, `'easy'`, `'normal'`, or `'hard'`
  * `game_hardcore` - boolean whether the game is in hardcore mode
  * `game_storage_format` - format of the world save files, either `'McRegion'` or `'Anvil'`
  * `game_default_gamemode` - default gamemode for new players
  * `game_max_players` - max allowed players when joining the world
  * `game_view_distance` - the view distance
  * `game_mod_name` - the name of the base mod. Expect `'fabric'`
  * `game_version` - base version of the game
  * `game_target` - target release version
  * `game_major_target` - major release target. For 1.12.2, that would be 12
  * `game_minor_release` - minor release target. For 1.12.2, that would be 2
  * `game_protocol` - protocol version number
  * `game_pack_version` - datapack version number
  * `game_data_version` - data version of the game. Returns an integer, so it can be compared.
  * `game_stable` - indicating if its a production release or a snapshot
  
##### Server related properties
 * `server_motd` - the motd of the server visible when joining
 * `server_ip` - IP adress of the game hosted
 * `server_whitelisted` - boolean indicating whether the access to the server is only for whitelisted players
 * `server_whitelist` - list of players allowed to log in
 * `server_banned_players` - list of banned player names
 * `server_banned_ips` - list of banned IP addresses
 * `server_dev_environment` - boolean indicating whether this server is in a development environment.
 * `server_mods` - map with all loaded mods mapped to their versions as strings
 * `server_last_tick_times` - Returns a 100-long array of recent tick times, in milliseconds. First item on the list is the most recent tick
If called outside of the main tick (either throgh scheduled tasks, or async execution), then the first item on the
list may refer to the previous tick performance. In this case the last entry (tick 100) would refer to the most current
tick. For all intent and purpose, `system_info('last_tick_times'):0` should be used as last tick execution time, but
individual tick times may vary greatly, and these need to be taken with the little grain of averaging.
 
##### Source related properties
 
 The source is what is the cause of the code running, with Carpet using it same way as Minecraft commands use to run. Those are used in
 some API functions that interact with the game or with commands, and can be manipulated if the execution is caused by an `execute` command, modified
 by some functions or ran in non-standard ways. This section provides useful information from these cases (like running from a command
 block, right clicking a sign, etc)
 * `source_entity` - The entity associated with the execution. This is usually a player (in which case `player()` would get the entity from this),
                         but it may also be a different entity or `null` if the execution comes from the server console or a command block.
 * `source_position` - The position associated with the execution. This is usually the position of the entity, but it may have been manipulated or
                           it could come from a command block (no entity then). If this call comes from the server console, it will be the world spawn.
 * `source_dimension` - The dimension associated with the execution. Execution from the server console provides `overworld` as the dimension.
                            This can be manipulated by running code inside `in_dimension()`.
 * `source_rotation` - The rotation associated with the execution. Usually `[0, 0]` in non-standard situations, the rotation of the entity otherwise.
 
##### System related properties
 * `java_max_memory` - maximum allowed memory accessible by JVM
 * `java_allocated_memory` - currently allocated memory by JVM
 * `java_used_memory` - currently used memory by JVM
 * `java_cpu_count` - number of processors
 * `java_version` - version of Java
 * `java_bits` - number indicating how many bits the Java has, 32 or 64
 * `java_system_cpu_load` - current percentage of CPU used by the system
 * `java_process_cpu_load` - current percentage of CPU used by JVM
 
##### Scarpet related properties
 * `scarpet_version` - returns the version of the carpet your scarpet comes with.

## NBT Storage

### `nbt_storage()`, `nbt_storage(key)`, `nbt_storage(key, nbt)`
Displays or modifies individual storage NBT tags. With no arguments, returns the list of current NBT storages. With specified `key`, returns the `nbt` associated with current `key`, or `null` if storage does not exist. With specified `key` and `nbt`, sets a new `nbt` value, returning previous value associated with the `key`.
NOTE: This NBT storage is shared with all vanilla datapacks and scripts of the entire server and is persistent between restarts and reloads. You can also access this NBT storage with vanilla `/data <get|modify|merge> storage <key> ...` command.
