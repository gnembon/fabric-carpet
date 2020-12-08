# Built in scarpet scripts

## For use in game:

### AI Tracker app (`ai_tracker.sc`)

#### Description

This is a scarpet app which allows you to see ai-related stuff for entities, eg. health, or item pickup radius. 
It comes with `/ai_tracker` command

#### Available commands:

 - `/ai_tracker toggle <display>`: This will toggle a visual display around entities. Note that labels on their heads
      will stack one above another and not overlap. Available options are:

    - `villager_iron_golem_spawning`: A box around villagers to show where golems can spawn, the number of ticks before
         they search again, the time since they've slept and the number of golems nearby
    - `pathfinding`: Squares on the floor showing where the entity wants to move
    - `velocity`: x, y, z and overall velocity, shown above entity's head
    - `villager_breeding`: A visual display above villager showing breeding stats, i.e 
    - `villager_buddy_detection`: A box around villagers showing where they detect buddies, and a number above their
         head to show how many they've found
    - `item_pickup`: A box around entity to show their item pickup range
    - `portal_cooldown`: Label above entity head to show portal cooldown
    - `health`: Label above entity showing health
    - `xpstack`: For 1.17+, shows the number of orbs stacked into the current orb.
    - `villager_hostile_detection <hostile>`: Shows the range around the villager in which it will be scared by a hostile.
         The hostiles that can scare villagers are: zombies(and variants), witches, zoglins, ravagers, vindicators, pillagers, ravagers,
         illusioners, vexes and evokers. If any of those mobs is in range (not just the one you have selected with the command),
         a red line will be drawn from it to the villager, and the label above the villager will go from `Peaceful` to 
         the name of the mob.
    - `boxes`: This toggles display of boxes and circles, leaving lines, labels and pathfinding squares.


 - `/ai_tracker clear`: This will clear all displays, including labels and lines, etc.
 - `/ai_tracker update_frequency <ticks>`: This changes the number of times per tick that updates to the shapes are sent
   to the player. Default (and recommended) setting is once evry 20 ticks, but you can change it with this command.
- `/ai_tracker transparency <alpha>`: This changes the transparency of displays.

### Camera app (`camera.sc`)

#### Description

This is a scarpet app which allows you to create a path and move along it. You cna do this in any gamemode, so you can 
even use it to interact with the world. The idea is to make timelapse recordings more spectacular.

#### Available commands:

 /camera start" - Set the starting point, resetting the path

 - `/camera add <N>`: Add a point to the end, <N> secs later
 - `/camera prepend <N>`: Prepend the start <N> secs before
 - `/camera clear`: Remove entire selected path
 - `/camera select`: Select a point you're looking at, or just punch it to select it
 - `/camera place_player`: Move player to the selected point
 - `/camera move`: Move the selected point to players location
 - `/camera duration <X>`: Set new selected path duration
 - `/camera split_point`: Split selected path in half.
 - `/camera delete_point`: Remove current key point
 - `/camera trim_path`: Remove all key points from selected up

 - `/camera save_as <name>`:
 - `/camera load <name>`: Store and load paths from world saves /scripts folder

 - `/camera interpolation <interpolation>`: Select interpolation between points:
   - `cr`: Catmull-Rom interpolation (default).
       smooth path that goes through all points.
   - `linear`: straight paths between points.
   - `gauss`: automatic smooth transitions.
   - `gauss <N>`: custom fixed variance 
           (in seconds) for special effects.
    
    Gauss makes the smoothest path, but treats points as suggestions only
 
 - `/camera repeat <N> <last_delay>`: Repeat existing points configuration n-times, using `<last_delay>` seconds to link path ends
 - `/camera stretch <factor>`: Change length of the entire path (in %) from 25 (4x faster), to 400 (4x slower)
 - `/camera transpose`: Move entire path with the start at players position.
 - `/camera play`: Run the path with the player. Use "sneak" to stop it prematurely, and run `/camera hide` + F1 to clear the view
 - `/camera show`: Show current path particles. Color of particles used is different for different players
 - `/camera hide`: Hide path display
 - `/camera prefer_smooth_play`: Eat CPU spikes and continue as usual
 - `/camera prefer_synced_play`: After CPU spikes jump to where you should be

### Chunk display app (`chunk_display.sc`)

#### Description

Right clicking with a stack of grass blocks will create a minimap above your head, showing OW chunks loaded around you. 
Grass will also be enchanted, and right clicking again will de-enchant it and get rid of display. Number of grass blocks 
in stack = number of chunk loading states shown around you.
Use netherrack and endstone for nether and end respectively
The colour of chunks in the display shows their load state:
 - Green: Loaded and entity processing
 - Red: Loaded and redstone processing
 - Yellow: Outer edge of loaded chunks
 - Checker pattern: Unstably generated
Ticket types:
 - Blue: Player or force-loaded (loaded practically the same way, i.e can never unload)
 - Purple: Dragon, portal or teleportation (you can tell by dimension)
 - Yellow: Light updates
 - Lime/green: Spawn chunks

### Distance app(`distance_beta.sc`)

#### Description

This is labelled beta as it's still being worked on

This app is meant to replace and improve upon existing `/distance` command. It can do everything the original can do, and
more. It can add a label at the position to see the distance, or draw a line, sphere or box, and even customize the block
with which you measure distance. Note though, that it will always draw labels in the centre of the block, even if the
block is opaque, so carpets and glass work best.

#### Available commands:

 - `/distance_beta from <from> to? <to>`: Sets the initial point to `from`, and can also set end point to `to` and calculate.
 - `/distance_beta to <to>`: Sets the end point to `to` and calculates from start point. If no start point is present, it will fail.
 - `/distance_beta clear`: Clears all displays and labels
 - `/distance_beta clear last`: Clears last display created by the player
 - `/distance_beta display <mode>`: Can be one of:
    - `chat`: Prints info to chat, as usual
    - `line`: Draws a line between start and end point
    - `centred_sphere`: Draws a sphere centred at the start position, which touches the end position on its surface area.
    - `box`: Draws a box with opposite corners between the start and end points
 - `/distance_beta assist <block>`: Sets the block which you can place to tell you distance. Suggests brown_carpet, but 
    takes any block
 - `/distance_beta assist none`: No block can be placed to display info (default).

### Draw command(`draw_beta.sc`)

#### Description

It's practically the same as the old `/draw` command, but it can also draw hollow shapes, and cache drawn shapes so it's
faster to draw them in the future.
NB: If drawing a radius 128 sphere for mob spawning range, note that blocks on the surface of the sphere may or may not
be spawnable, but blocks outside will be 100% spawn proof

#### Available commands:

 - `/draw_beta sphere <centre> <radius> <block> replace? <replacement?>`: Draws a sphere centred at `centre`, with 
    radius `radius`, made out of `block`, possibly replacing `replacement`
 - `/draw_beta ball <centre> <radius> <block> replace? <replacement?>`: Draws a ball (filled sphere) centred at `centre`,
   with radius `radius`, made out of `block`, possibly replacing `replacement`
 - `/draw_beta diamond <centre> <radius> <block> replace? <replacement?>`:Draws a diamond centred at `centre`, with
   radius `radius`, made out of `block`, possibly replacing `replacement`
 - `/draw_beta pyramid/cone <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace? <replacement?>`:
    Draws a cone/pyramid with above parameters. `pointing` can be either up or down, and `orientation` can be x, y or z.
   `hollow` can either be `hollow` or `solid`
 - `/draw_beta cuboid/cylinder <center> <radius> <height> <orientation> <block> <hollow> replace? <replacement?>`: Draws
    a cylinder/cuboid with the above params. `orientation` is always x, y or z. `hollow` can either be `hollow` or `solid`
 - `/draw_beta cache <mode>`: Select the cache mode, one of:
    - `never`: Doesn't cache shapes, just recalculates every time. It also avoids creating the cache file. Not recommended,
        but available nonetheless
    - `ingame`: Caches the shapes drawn within games, but doesn't cache them across restarts. It's the default option.
    - `always`: Caches the shapes, and on server close also saves them to disk, so
 - `/draw_beta cache clear`: Clears cache and deletes cache file
 - `/draw_beta cache save`: Saves current cache to disk.
Legend speaks of a last, hidden command, which few know about. It shows itself on occasion, but that is yours to find...

### Overlay app (`overlay.sc`)

#### Description

This app, like ai_tracker, provides useful overlays for use in game, but for world info as opposed to entity info.

#### Available commands:

 - `/overlay structure <structure>`: Shows the bounding box of all in-game structures `('monument', 'fortress', 'mansion',
    'jungle_pyramid', 'desert_pyramid', 'endcity', 'igloo', 'shipwreck', 'swamp_hut', 'stronghold', 'ocean_ruin',
    'buried_treasure', 'pillager_outpost', 'mineshaft', 'village', 'nether_fossil', 'bastion_remnant', 'ruined_portal'`).
 - `/overlay slime_chunks`: Draws slime chunks
 - `/overlay portal coordinates`: Shows equivalent coordinate in the nether/overworld.
 - `/overlay portal links`: IDK
 - `/overlay <shape> <radius> at <player> <color?>`: Draws a shape (currently sphere or box) at current player position,
    with optional colour
 - `/overlay <shape> <radius> following <player> <color?>`: Draws a shape (currently sphere or box) set to follow a player,
    with optional colour.
   
## For use in app development:

### Event/Stats test(`event_test.sc`,`stats_test.sc`)

#### Description

These detect when scarpet-trackable events occur, or an action occurs which causes a stat to change. They will log all
the changes.

### Math(s) (`math.scl`)

### Description

This app contains a bunch of useful maths functions for scarpet apps. They can be imported via
`import('math','func1','func2',...)`.

#### Available functions:

 - `_euclidean_sq(vec1, vec2)`:Gets the square of the distance between two points
 - `_euclidean(vec1, vec2)`:Gets the distance between two points
 - `_manhattan(vec1, vec2)`:Gets the taxicab distance between two points
 - `_vec_length(vec)`:Gets magnitude of a vector
 - `_round(num,precision)`:Rounds a num to a precision

