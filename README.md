# Fabric Carpet

Cause all carpets are made of fabric?

# How? Hwat?

Follow instructions for all other fabric mods in https://fabricmc.net/use/ and dump carpet...jar in `mods` folder along
with other compatible mods.

# Carpet Mod Settings
## antiCheatDisabled
Prevents players from rubberbanding when moving too fast  
... or being kicked out for 'flying'  
Puts more trust in clients positioning  
Increases player allowed mining distance to 32 blocks  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`, `SURVIVAL`  
  
## carpets
Placing carpets may issue carpet commands for non-op players  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `SURVIVAL`  
  
## combineXPOrbs
XP orbs combine with other into bigger orbs  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## commandCameramode
Enables /c and /s commands to quickly switch between camera and survival modes  
/c and /s commands are available to all players regardless of their permission levels  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandDistance
Enables /distance command to measure in game distance between points  
Also enables brown carpet placement action if 'carpets' rule is turned on as well  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandDraw
Enables /draw commands  
... allows for drawing simple shapes  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandInfo
Enables /info command for blocks  
Also enables gray carpet placement action  
if 'carpets' rule is turned on as well  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandLog
Enables /log command to monitor events via chat and overlays  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandPerimeterInfo
Enables /perimeterinfo command  
... that scans the area around the block for potential spawnable spots  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandPlayer
Enables /player command to control/spawn players  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandScript
Enables /script command  
An in-game scripting API for Scarpet programming language  
* Type: `boolean`  
* Default value: `true`  
* Required options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  
## commandSpawn
Enables /spawn command for spawn tracking  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandTick
Enables /tick command to control game clocks  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## commandTrackAI
Allows to track mobs AI via /track command  
* Type: `String`  
* Default value: `true`  
* Suggested options: `true`, `false`  
* Categories: `COMMAND`  
* Additional notes:  
  * It has an accompanying command  
  * Can be limited to 'ops' only, or a custom permission level  
  
## ctrlQCraftingFix
Dropping entire stacks works also from on the crafting UI result slot  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `BUGFIX`, `SURVIVAL`  
  
## customMOTD
Sets a different motd message on client trying to connect to the server  
use '_' to use the startup setting from server.properties  
* Type: `String`  
* Default value: `_`  
* Suggested options: `_`  
* Categories: `CREATIVE`  
  
## defaultLoggers
sets these loggers in their default configurations for all new players  
use csv, like 'tps,mobcaps' for multiple loggers, none for nothing  
* Type: `String`  
* Default value: `none`  
* Suggested options: `none`, `tps`, `mobcaps,tps`  
* Categories: `CREATIVE`, `SURVIVAL`  
  
## desertShrubs
Saplings turn into dead shrubs in hot climates and no water access  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## disableSpawnChunks
Allows spawn chunks to unload  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## explosionNoBlockDamage
Explosions won't destroy blocks  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`, `TNT`  
  
## extremeBehaviours
Edge cases are as frequent as common cases, for testing only!!  
Dispensers and Droppers will use extreme random velocity values  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## fastRedstoneDust
Lag optimizations for redstone dust  
by Theosib  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `EXPERIMENTAL`, `OPTIMIZATION`  
  
## fillLimit
Customizable fill/clone volume limit  
* Type: `int`  
* Default value: `32768`  
* Suggested options: `32768`, `250000`, `1000000`  
* Categories: `CREATIVE`  
* Additional notes:  
  * You must choose a value from 1 to 20M  
  
## fillUpdates
fill/clone/setblock and structure blocks cause block updates  
* Type: `boolean`  
* Default value: `true`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## flatWorldStructureSpawning
Allows structure mobs to spawn in flat worlds  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `EXPERIMENTAL`, `CREATIVE`  
  
## flippinCactus
Players can flip and rotate blocks when holding cactus  
Doesn't cause block updates when rotated/flipped  
Applies to pistons, observers, droppers, repeaters, stairs, glazed terracotta etc...  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`, `SURVIVAL`, `FEATURE`  
  
## hardcodeTNTangle
Sets the horizontal random angle on TNT for debugging of TNT contraptions  
Set to -1 for default behavior  
* Type: `double`  
* Default value: `0.0`  
* Suggested options: `-1`  
* Categories: `TNT`  
* Additional notes:  
  * Must be between 0 and 2pi, or -1  
  
## hopperCounters
hoppers pointing to wool will count items passing through them  
Enables /counter command, and actions while placing red and green carpets on wool blocks  
Use /counter <color?> reset to reset the counter, and /counter <color?> to query  
In survival, place green carpet on same color wool to query, red to reset the counters  
Counters are global and shared between players, 16 channels available  
Items counted are destroyed, count up to one stack per tick per hopper  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `COMMAND`, `CREATIVE`, `FEATURE`  
* Additional notes:  
  * It has an accompanying command  
  
## huskSpawningInTemples
Only husks spawn in desert temples  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## lagFreeSpawning
Spawning requires much less CPU and Memory  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `OPTIMIZATION`  
  
## leadFix
Fixes leads breaking/becoming invisible in unloaded chunks  
You may still get visibly broken leash links on the client side, but server side the link is still there.  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `BUGFIX`  
  
## maxEntityCollisions
Customizable maximal entity collision limits, 0 for no limits  
* Type: `int`  
* Default value: `0`  
* Suggested options: `0`, `1`, `20`  
* Categories: `OPTIMIZATION`  
* Additional notes:  
  * Must be a positive number  
  
## mergeTNT
Merges stationary primed TNT entities  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `TNT`  
  
## missingTools
Pistons, Glass and Sponge can be broken faster with their appropriate tools  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `SURVIVAL`  
  
## movableBlockEntities
Pistons can push block entities, like hoppers, chests etc.  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `EXPERIMENTAL`, `FEATURE`  
  
## onePlayerSleeping
One player is required on the server to cause night to pass  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `SURVIVAL`  
  
## optimizedTNT
TNT causes less lag when exploding in the same spot and in liquids  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `TNT`  
  
## persistentParrots
Parrots don't get of your shoulders until you receive proper damage  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `SURVIVAL`, `FEATURE`  
  
## placementRotationFix
fixes block placement rotation issue when player rotates quickly while placing blocks  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `BUGFIX`  
  
## portalCreativeDelay
Portals won't let a creative player go through instantly  
Holding obsidian in either hand won't let you through at all  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## portalSuffocationFix
Nether portals correctly place entities going through  
Entities shouldn't suffocate in obsidian  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `BUGFIX`  
  
## pushLimit
Customizable piston push limit  
* Type: `int`  
* Default value: `12`  
* Suggested options: `10`, `12`, `14`, `100`  
* Categories: `CREATIVE`  
* Additional notes:  
  * You must choose a value from 1 to 1024  
  
## quasiConnectivity
Pistons, droppers and dispensers react if block above them is powered  
* Type: `boolean`  
* Default value: `true`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## railPowerLimit
Customizable powered rail power range  
* Type: `int`  
* Default value: `9`  
* Suggested options: `9`, `15`, `30`  
* Categories: `CREATIVE`  
* Additional notes:  
  * You must choose a value from 1 to 1024  
  
## renewableCoral
Coral structures will grow with bonemeal from coral plants  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## renewableSponges
Guardians turn into Elder Guardian when struck by lightning  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## rotatorBlock
Cactus in dispensers rotates blocks.  
Rotates block anti-clockwise if possible  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`, `DISPENSER`  
  
## scriptsAutoload
Scarpet script from world files will autoload on server/world start   
if /script is enabled  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## shulkerSpawningInEndCities
Shulkers will respawn in end cities  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## silverFishDropGravel
Silverfish drop a gravel item when breaking out of a block  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `FEATURE`  
  
## smoothClientAnimations
smooth client animations with low tps settings  
works only in SP, and will slow down players  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## stackableShulkerBoxes
Empty shulker boxes can stack to 64 when dropped on the ground  
To move them around between inventories, use shift click to move entire stacks  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `SURVIVAL`, `FEATURE`  
  
## summonNaturalLightning
summoning a lightning bolt has all the side effects of natural lightning  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  
  
## superSecretSetting
Gbhs sgnf sadsgras fhskdpri!  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `EXPERIMENTAL`  
  
## tntDoNotUpdate
TNT doesn't update when placed against a power source  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`, `TNT`  
  
## tntPrimerMomentumRemoved
Removes random TNT momentum when primed  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`, `TNT`  
  
## tntRandomRange
Sets the tnt random explosion range to a fixed value  
Set to -1 for default behavior  
* Type: `double`  
* Default value: `-1.0`  
* Suggested options: `-1`  
* Categories: `TNT`  
* Additional notes:  
  * optimizedTNT must be enabled  
  * Cannot be negative, except for -1  
  
## unloadedEntityFix
Entities pushed or moved into unloaded chunks no longer disappear  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `EXPERIMENTAL`, `BUGFIX`  
  
## viewDistance
Changes the view distance of the server.  
Set to 0 to not override the value in server settings.  
* Type: `int`  
* Default value: `0`  
* Suggested options: `0`, `12`, `16`, `32`  
* Categories: `CREATIVE`  
* Additional notes:  
  * You must choose a value from 0 (use server settings) to 32  
  
## xpNoCooldown
Players absorb XP instantly, without delay  
* Type: `boolean`  
* Default value: `false`  
* Required options: `true`, `false`  
* Categories: `CREATIVE`  

