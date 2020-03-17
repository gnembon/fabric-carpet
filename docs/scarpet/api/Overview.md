# Minecraft specific API and `scarpet` language add-ons and commands

Here is the gist of the Minecraft related functions. Otherwise the CarpetScript could live without Minecraft.

## Dimension warning

One note, which is important is that most of the calls for entities and blocks would refer to the current 
dimension of the caller, meaning, that if we for example list all the players using `player('all')` function, 
if a player is in the other dimension, calls to entities and blocks around that player would point incorrectly. 
Moreover, running commandblocks in the spawn chunks would mean that commands will always refer to the overworld 
blocks and entities. In case you would want to run commands across all dimensions, just run three of them, 
using `/execute in overworld/the_nether/the_end run script run ...` and query players using `player('*')`, 
which only returns players in current dimension, or use `in_dimension(expr)` function.