# Inventory and Items API

## Manipulating inventories of blocks and entities

Most functions in this category require inventory as the first argument. Inventory could be specified by an entity, 
or a block, or position (three coordinates) of a potential block with inventory, or can be preceded with inventory
type.
Inventory type can be `null` (default), `'enderchest'` denoting player enderchest storage, or `'equipment'` applying to 
entities hand and armour pieces. Then the type can be followed by entity, block or position coordinates.
For instance, player enderchest inventory requires 
two arguments, keyword `'enderchest'`, followed by the player entity argument, (or a single argument as a string of a
form: `'enderchest_steve'` for legacy support). If your player name starts with `'enderchest_'`, first of all, tough luck, 
but then it can be always accessed by passing a player
entity value. If all else fails, it will try to identify first three arguments as coordinates of a block position of
a block inventory. Player inventories can also be called by their name. 

A few living entities can have both: their regular inventory, and their equipment inventory. 
Player's regular inventory already contains the equipment, but you can access the equipment part as well, as well as 
their enderchest separately. For entity types that only have
their equipment inventory, the equipment is returned by default (`null` type).

If that's confusing see examples under `inventory_size` on how to access inventories. All other `inventory_...()` functions 
use the same scheme.

 
 If the entity or a block doesn't have 
an inventory, all API functions typically do nothing and return null.

Most items returned are in the form of a triple of item name, count, and nbt or the extra data associated with an item. 

### `item_list(tag?)`

With no arguments, returns a list of all items in the game. With an item tag provided, list items matching the tag, or `null` if tag is not valid.

### `item_tags(item, tag?)`

Returns list of tags the item belongs to, or, if tag is provided, `true` if an item maches the tag, `false` if it doesn't and `null` if that's not a valid tag

Throws `unknown_item` if item doesn't exist.

### `stack_limit(item)`

Returns number indicating what is the stack limit for the item. Its typically 1 (non-stackable), 16 (like buckets), 
or 64 - rest. It is recommended to consult this, as other inventory API functions ignore normal stack limits, and 
it is up to the programmer to keep it at bay. As of 1.13, game checks for negative numbers and setting an item to 
negative is the same as empty.

Throws `unknown_item` if item doesn't exist.

<pre>
stack_limit('wooden_axe') => 1
stack_limit('ender_pearl') => 16
stack_limit('stone') => 64
</pre>

### `item_category(item)`

Returns the string representing the category of a given item, like `building_blocks`, `combat`, or `tools`.

Throws `unknown_item` if item doesn't exist.

<pre>
item_category('wooden_axe') => tools
item_category('ender_pearl') => misc
item_category('stone') => building_blocks
</pre>

### `recipe_data(item, type?)`, `recipe_data(recipe, type?)`

returns all recipes matching either an `item`, or represent actual `recipe` name. In vanilla datapack, for all items
that have one recipe available, the recipe name is the same as the item name but if an item has multiple recipes, its
direct name can be different.

Recipe type can take one of the following options:
 * `'crafting'` - default, crafting table recipe
 * `'smelting'` - furnace recipe
 * `'blasting'` - blast furnace recipe
 * `'smoking'` - smoker recipe
 * `'campfire_cooking'` - campfire recipe
 * `'stonecutting'` - stonecutter recipe
 * `'smithing'` - smithing table (1.16+)
 
 The return value is a list of available recipes (even if there is only one recipe available). Each recipe contains of
 an item triple of the crafting result, list of ingredients, each containing a list of possible variants of the
 ingredients in this slot, as item triples, or `null` if its a shaped recipe and a given slot in the patterns is left
 empty, and recipe specification as another list. Possible recipe specs is:
  * `['shaped', width, height]` - shaped crafting. `width` and `height` can be 1, 2 or 3.
  * `['shapeless']` - shapeless crafting
  * `['smelting', duration, xp]` - smelting/cooking recipes
  * `['cutting']` - stonecutter recipe
  * `['special']` - special crafting recipe, typically not present in the crafting menu
  * `['custom']` - other recipe types
  
Note that ingredients are specified as tripes, with count and nbt information. Currently all recipes require always one
of the ingredients, and for some recipes, even if the nbt data for the ingredient is specified (e.g. `dispenser`), it
can accept items of any tags.

Also note that some recipes leave some products in the crafting window, and these can be determined using
 `crafting_remaining_item()` function 
  
 Examples:
 <pre>
 recipe_data('iron_ingot_from_nuggets')
 recipe_data('iron_ingot')
 recipe_data('glass', 'smelting')
 </pre>

### `crafting_remaining_item(item)`

returns `null` if the item has no remaining item in the crafting window when used as a crafting ingredient, or an
item name that serves as a replacement after crafting is done. Currently it can only be buckets and glass bottles.

### `inventory_size(inventory)`

Returns the size of the inventory for the entity or block in question. Returns null if the block or entity don't 
have an inventory.

<pre>
inventory_size(player()) => 41
inventory_size('enderchest', player()) => 27 // enderchest
inventory_size('equipment', player()) => 6 // equipment
inventory_size(null, player()) => 41  // default inventory for players

inventory_size(x,y,z) => 27 // chest
inventory_size(block(pos)) => 5 // hopper

horse = spawn('horse', x, y, z);
inventory_size(horse); => 2 // default horse inventory
inventory_size('equipment', horse); => 6 // unused horse equipment inventory
inventory_size(null, horse); => 2 // default horse

creeper = spawn('creeper', x, y, z);
inventory_size(creeper); => 6 // default creeper inventory is equipment since it has no other
inventory_size('equipment', creeper); => 6 // unused horse equipment inventory
inventory_size(null, creeper); => 6 // creeper default is its equipment
</pre>

### `inventory_has_items(inventory)`

Returns true, if the inventory is not empty, false if it is empty, and null, if its not an inventory.

<pre>    inventory_has_items(player()) => true
    inventory_has_items(x,y,z) => false // empty chest
    inventory_has_items(block(pos)) => null // stone
</pre>

### `inventory_get(inventory, slot)`

Returns the item in the corresponding inventory slot, or null if slot empty or inventory is invalid. You can use 
negative numbers to indicate slots counted from 'the back'.

<pre>
inventory_get(player(), 0) => null // nothing in first hotbar slot
inventory_get(x,y,z, 5) => ['stone', 1, {}]
inventory_get(player(), -1) => ['diamond_pickaxe', 1, {Damage:4}] // slightly damaged diamond pick in the offhand
</pre>

### `inventory_set(inventory, slot, count, item?, nbt?)`

Modifies or sets a stack in inventory. specify count 0 to empty the slot. If item is not specified, keeps existing 
item, just modifies the count. If item is provided - replaces current item. If nbt is provided - adds a tag to the 
stack at slot. Returns previous stack in that slot.

<pre>
inventory_set(player(), 0, 0) => ['stone', 64, {}] // player had a stack of stone in first hotbar slot
inventory_set(player(), 0, 6) => ['diamond', 64, {}] // changed stack of diamonds in player slot to 6
inventory_set(player(), 0, 1, 'diamond_axe','{Damage:5}') => null //added slightly damaged diamond axe to first player slot
</pre>

### `inventory_find(inventory, item, start_slot?, ), inventory_find(inventory, null, start_slot?)`

Finds the first slot with a corresponding item in the inventory, or if queried with null: the first empty slot. 
Returns slot number if found, or null otherwise. Optional start_slot argument allows to skip all preceeding slots 
allowing for efficient (so not slot-by-slot) inventory search for items.

<pre>
inventory_find(player(), 'stone') => 0 // player has stone in first hotbar slot
inventory_find(player(), null) => null // player's inventory has no empty spot
while( (slot = inventory_find(p, 'diamond', slot)) != null, 41, drop_item(p, slot) )
    // spits all diamonds from player inventory wherever they are
inventory_drop(x,y,z, 0) => 64 // removed and spawned in the world a full stack of items
</pre>

Throws `unknown_item` if item doesn't exist.

### `inventory_remove(inventory, item, amount?)`

Removes amount (defaults to 1) of item from inventory. If the inventory doesn't have the defined amount, nothing 
happens, otherwise the given amount of items is removed wherever they are in the inventory. Returns boolean 
whether the removal operation was successful. Easiest way to remove a specific item from player inventory 
without specifying the slot.

<pre>
inventory_remove(player(), 'diamond') => 1 // removed diamond from player inventory
inventory_remove(player(), 'diamond', 100) => 0 // player doesn't have 100 diamonds, nothing happened
</pre>

### `drop_item(inventory, slot, amount?, )`

Drops the items from indicated inventory slot, like player that Q's an item or villager, that exchanges food. 
You can Q items from block inventories as well. default amount is 0 - which is all from the slot. 
NOTE: hoppers are quick enough to pick all the queued items from their inventory anyways. 
Returns size of the actual dropped items.

<pre>
inventory_drop(player(), 0, 1) => 1 // Q's one item on the ground
inventory_drop(x,y,z, 0) => 64 // removed and spawned in the world a full stack of items
</pre>
