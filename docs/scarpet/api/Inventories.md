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

## Screens

A screen is a value type used to open screens for a player and interact with them.
For example, this includes the chest inventory gui, the crafting table gui and many more.

### `create_screen(player, type, name, callback?)`

Creates and opens a screen for a `player`.

Available `type`s:

* `anvil`
* `beacon`
* `blast_furnace`
* `brewing_stand`
* `cartography_table`
* `crafting`
* `enchantment`
* `furnace`
* `generic_3x3`
* `generic_9x1`
* `generic_9x2`
* `generic_9x3`
* `generic_9x4`
* `generic_9x5`
* `generic_9x6`
* `grindstone`
* `hopper`
* `lectern`
* `loom`
* `merchant`
* `shulker_box`
* `smithing`
* `smoker`
* `stonecutter`

The `name` parameter can be a formatted text and will be displayed at the top of the screen.
Some screens like the lectern or beacon screen don't show it.

Optionally, a `callback` function can be passed as the fourth argument.
This functions needs to have four parameters:
`_(screen, player, action, data) -> ...`

The `screen` parameter is the screen value of the screen itself.
`player` is the player who interacted with the screen.
`action` is a string corresponding to the interaction type.
Can be any of the following:

Slot interactions:

* `pickup`
* `quick_move`
* `swap`
* `clone`
* `throw`
* `quick_craft`
* `pickup_all`

The `data` for this interaction is a map, with a `slot` and `button` value.
`slot` is the slot index of the slot that was clicked.
When holding an item in the cursor stack and clicking inside the screen,
but not in a slot, this is -1.
If clicked outside the screen (where it would drop held items), this value is null.
The `button` is the mouse button used to click the slot.

For the `swap` action, the `button` is the number key 0-8 for a certain hotbar slot.

For the `quick_craft` action, the `data` also contains the `quick_craft_stage`,
which is either 0 (beginning of quick crafting), 1 (adding item to slot) or 2 (end of quick crafting).

Other interactions:

* `button` Pressing a button in certain screens that have button elements (enchantment table, lectern, loom and stonecutter)
The `data` provides a `button`, which is the index of the button that was pressed.
Note that for lecterns, this index can be certain a value above 100, for jumping to a certain page.
This can come from formatted text inside the book, with a `change_page` click event action.

* `close` Triggers when the screen gets closed. No `data` provided.

* `select_recipe` When clicking on a recipe in the recipe book.
`data` contains a `recipe`, which is the identifier of the clicked recipe,
as well as `craft_all`, which is a boolean specifying whether
shift was pressed when selecting the recipe.

* `slot_update` Gets called **after** a slot has changed contents. `data` provides a `slot` and `stack`.

By returning a string `'cancel'` in the callback function,
the screen interaction can be cancelled.
This doesn't work for the `close` action.

The `create_screen` function returns a `screen` value,
which can be used in all inventory related functions to access the screens' slots.
The screen inventory covers all slots in the screen and the player inventory.
The last slot is the cursor stack of the screen,
meaning that using `-1` can be used to modify the stack the players' cursor is holding.

### `close_screen(screen)`

Closes the screen of the given screen value.
Returns `true` if the screen was closed.
If the screen is already closed, returns `false`.

### `screen_property(screen, property)`

### `screen_property(screen, property, value)`

Queries or modifies a certain `property` of a `screen`.
The `property` is a string with the name of the property.
When called with `screen` and `property` parameter, returns the current value of the property.
When specifying a `value`,
the property will be assigned the new `value` and synced with the client.

**Options for `property` string:**

| `property` | Required screen type | Type | Description |
|---|---|---|---|
| `name` | **All** | text | The name of the screen, as specified in the `create_screen()` function. Can only be queried. |
| `open` | **All** | boolean | Returns `true` if the screen is open, `false` otherwise. Can only be queried. |
| `fuel_progress` | furnace/smoker/blast_furnace | number | Current value of the fuel indicator. |
| `max_fuel_progress` | furnace/smoker/blast_furnace | number | Maximum value for the full fuel indicator. |
| `cook_progress` | furnace/smoker/blast_furnace | number | Cooking progress indicator value. |
| `max_cook_progress` | furnace/smoker/blast_furnace | number | Maximum value for the cooking progress indicator. |
| `level_cost` | anvil | number | Displayed level cost for the anvil. |
| `page` | lectern | number | Opened page in the lectern screen. |
| `beacon_level` | beacon | number | The power level of the beacon screen. This affects how many effects under primary power are grayed out. Should be a value between 0-5. |
| `primary_effect` | beacon | number | The effect id of the primary effect. This changes the effect icon on the button on the secondary power side next to the regeneration effect. |
| `secondary_effect` | beacon | number | The effect id of the secondary effect. This seems to change nothing, but it exists. |
| `brew_time` | brewing_stand | number | The brewing time indicator value. This goes from 0 to 400. |
| `brewing_fuel` | brewing_stand | number | The fuel indicator progress. Values range between 0 to 20. |
| `enchantment_power_x` | enchantment | number | The level cost of the shown enchantment. Replace `x` with 1, 2 or 3 (e.g. `enchantment_power_2`) to target the first, second or third enchantment. |
| `enchantment_id_x` | enchantment | number | The id of the enchantment shown (replace `x` with the enchantment slot 1/2/3). |
| `enchantment_level_x` | enchantment | number | The enchantment level of the enchantment. |
| `enchantment_seed` | enchantment | number | The seed of the enchanting screen. This affects the text shown in the standard Galactic alphabet. |
| `banner_pattern` | loom | number | The selected banner pattern inside the loom. |
| `stonecutter_recipe` | stonecutter | number | The selected recipe in the stonecutter. |

### Screen example scripts

<details>
<summary>Chest click event</summary>

```py
__command() -> (
    create_screen(player(),'generic_9x6',format('db Test'),_(screen, player, action, data) -> (
        print(player('all'),str('%s\n%s\n%s',player,action,data)); //for testing
        if(action=='pickup',
            inventory_set(screen,data:'slot',1,if(inventory_get(screen,data:'slot'),'air','red_stained_glass_pane'));
        );
        'cancel'
    ));
);
```
</details>

<details>
<summary>Anvil text prompt</summary>

```py
// anvil text prompt gui
__command() -> (
    global_screen = create_screen(player(),'anvil',format('r Enter a text'),_(screen, player, action, data)->(
        if(action == 'pickup' && data:'slot' == 2,
            renamed_item = inventory_get(screen,2);
            nbt = renamed_item:2;
            name = parse_nbt(nbt:'display':'Name'):'text';
            if(!name, return('cancel')); //don't accept empty string
            print(player,'Text: ' + name);
            close_screen(screen);
        );
        'cancel'
    ));
    inventory_set(global_screen,0,1,'paper','{display:{Name:\'{"text":""}\'}}');
);

```
</details>

<details>
<summary>Lectern flip book</summary>

```py
// flip book lectern

global_fac = 256/60;
curve(v) -> (
    v = v%360;
    if(v<60,v*global_fac,v<180,255,v<240,255-(v-180)*global_fac,0);
);

hex_from_hue(hue) -> str('#%02X%02X%02X',curve(hue+120),curve(hue),curve(hue+240));

make_char(hue) -> str('{"text":"▉","color":"%s"}',hex_from_hue(hue));

make_page(hue) -> (
    page = '[';
    loop(15, //row
        y = _;
        loop(14, //col
            x = _;
            page += make_char(hue+x*4+y*4) + ',';
        );
    );
    return(slice(page,0,-2)+']');
);


__command() -> (
    screen = create_screen(player(),'lectern','Lectern example (this text is not visible)',_(screen, player, action, data)->(
        if(action=='button',
            print(player,'Button: ' + data:'button');
        );
        'cancel'
    ));

    page_count = 60;
    pages = [];

    loop(page_count,
        hue = _/page_count*360;
        pages += make_page(hue);
    );

    nbt = encode_nbt({
        'pages'-> pages,
        'author'->'-',
        'title'->'-',
        'resolved'->1
    });

    inventory_set(screen,0,1,'written_book',nbt);

    task(_(outer(screen),outer(page_count))->(
        while(screen != null && screen_property(screen,'open'),100000,
            p = (p+1)%page_count;
            screen_property(screen,'page',p);
            sleep(50);
        );
    ));
);

```
</details>

<details>
<summary>generic_3x3 cursor stack</summary>

```py
__command() -> (
    screen = create_screen(player(),'generic_3x3','Title',_(screen, player, action, data) -> (
        if(action=='pickup',
            // set slot to the cursor stack item
            inventory_set(screen,data:'slot',1,inventory_get(screen,-1):0);
        );
        'cancel'
    ));

    task(_(outer(screen))->(
        // keep the cursor stack item blinking
        while(screen_property(screen,'open'),100000,
            inventory_set(screen,-1,1,'red_concrete');
            sleep(500);
            inventory_set(screen,-1,1,'lime_concrete');
            sleep(500);
        );
    ));
);
```
</details>