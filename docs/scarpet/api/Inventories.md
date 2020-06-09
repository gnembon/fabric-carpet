# Inventory and Items API

## Manipulating inventories of blocks and entities

Most functions in this category require inventory as the first argument. Inventory could be specified by an entity, 
or a block, or position (three coordinates) of a potential block with inventory. Player enderchest inventory require 
two arguments, keyword `'enderchest'`, followed by the player entity argument, or a single argument as a string of a
form: `'enderchest_steve'`. If your player name starts with enderchest, it can be always accessed by passing a player
entity value. If all else fails, it will try to identify first three arguments as coordinates of a block position of
a block inventory. Player inventories can also be called by their name.
 
 If the entity or a block doesn't have 
an inventory, they typically do nothing and return null.

Most items returned are in the form of a triple of item name, count, and nbt or the extra data associated with an item. 
Manipulating of the nbt data can be costly, but retrieving them from the tuple to match other aspects is cheap

### `stack_limit(item)`

Returns number indicating what is the stack limit for the item. Its typically 1 (non-stackable), 16 (like buckets), 
or 64 - rest. It is recommended to consult this, as other inventory API functions ignore normal stack limits, and 
it is up to the programmer to keep it at bay. As of 1.13, game checks for negative numbers and setting an item to 
negative is the same as empty.

<pre>
stack_limit('wooden_axe') => 1
stack_limit('ender_pearl') => 16
stack_limit('stone') => 64
</pre>

### `item_category(item)`

Returns the string representing the category of a given item, like `building_blocks`, `combat`, or `tools`.

<pre>
item_category('wooden_axe') => tools
item_category('ender_pearl') => misc
item_category('stone') => building_blocks
</pre>

### `inventory_size(inventory)`

Returns the size of the inventory for the entity or block in question. Returns null if the block or entity don't 
have an inventory

<pre>
inventory_size(player()) => 41
inventory_size('enderchest', player()) => 27 // enderchest
inventory_size(x,y,z) => 27 // chest
inventory_size(block(pos)) => 5 // hopper
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
inventory_set(player(), 0, 1, 'diamond_axe','{Damage:5}') => null //added slightly damaged pick to first player slot
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
