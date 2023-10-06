# Scoreboard

### `scoreboard()`, `scoreboard(objective)`, `scoreboard(objective, key)`, `scoreboard(objective, key, value)`

Displays or modifies individual scoreboard values. With no arguments, returns the list of current objectives.
With specified `objective`, lists all keys (players) associated with current objective, or `null` if objective does not exist.
With specified `objective` and
`key`, returns current value of the objective for a given player (key). With additional `value` sets a new scoreboard
 value, returning previous value associated with the `key`. If the `value` is null, resets the scoreboard value.
 
### `scoreboard_add(objective, criterion?)`

Adds a new objective to scoreboard. If `criterion` is not specified, assumes `'dummy'`.
Returns `true` if the objective was created, or `null` if an objective with the specified name already exists.

Throws `unknown_criterion` if criterion doesn't exist.

<pre>
scoreboard_add('counter')
scoreboard_add('lvl','level')
</pre>

### `scoreboard_remove(objective)` `scoreboard_remove(objective, key)`

Removes an entire objective, or an entry in the scoreboard associated with the key. 
Returns `true` if objective has existed and has been removed, or previous
value of the scoreboard if players score is removed. Returns `null` if objective didn't exist, or a key was missing
for the objective.

### `scoreboard_display(place, objective)`

Sets display location for a specified `objective`. If `objective` is `null`, then display is cleared. If objective is invalid,
returns `null`.

### `scoreboard_property(objective, property)` `scoreboard_property(objective, property, value)`

Reads a property of an `objective` or sets it to a `value` if specified. Available properties are:

* `criterion`
* `display_name` (Formatted text supported)
* `display_slot`: When reading, returns a list of slots this objective is displayed in, when modifying, displays the objective in the specified slot
* `render_type`: Either `'integer'` or `'hearts'`, defaults to `'integer'` if invalid value specified

# Team

### `team_list()`, `team_list(team)`

Returns all available teams as a list with no arguments.

When a `team` is specified, it returns all the players inside that team. If the `team` is invalid, returns `null`.

### `team_add(team)`, `team_add(team,player)`

With one argument, creates a new `team` and returns its name if successful, or `null` if team already exists.


`team_add('admin')` -> Create a team with the name 'admin'
`team_add('admin','Steve')` -> Joing the player 'Steve' into the team 'admin'

If a `player` is specified, the player will join the given `team`. Returns `true` if player joined the team, or `false` if nothing changed since the player was already in this team. If the team is invalid, returns `null`

### `team_remove(team)`

Removes a `team`. Returns `true` if the team was deleted, or `null` if the team is invalid.

### `team_leave(player)`

Removes the `player` from the team he is in. Returns `true` if the player left a team, otherwise `false`.

`team_leave('Steve')` -> Removes Steve from the team he is currently in
`for(team_list('admin'), team_leave('admin', _))` -> Remove all players from team 'admin'

### `team_property(team,property,value?)`

Reads the `property` of the `team` if no `value` is specified. If a `value` is added as a third argument, it sets the `property` to that `value`.

* `collisionRule`
  * Type: String
  * Options: always, never, pushOtherTeams, pushOwnTeam
    
* `color`
  * Type: String
  * Options: See [team command](https://minecraft.wiki/w/Commands/team#Arguments) (same strings as `'teamcolor'` [command argument](https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/Full.md#command-argument-types) options)

* `displayName`
  * Type: String or FormattedText, when querying returns FormattedText
  
* `prefix`
  * Type: String or FormattedText, when querying returns FormattedText

* `suffix`
  * Type: String or FormattedText, when querying returns FormattedText

* `friendlyFire`
  * Type: boolean
  
* `seeFriendlyInvisibles`
  * Type: boolean
  
* `nametagVisibility`
  * Type: String
  * Options: always, never, hideForOtherTeams, hideForOwnTeam

* `deathMessageVisibility`
  * Type: String
  * Options: always, never, hideForOtherTeams, hideForOwnTeam

Examples:

```
team_property('admin','color','dark_red')                 Make the team color for team 'admin' dark red
team_property('admin','prefix',format('r Admin | '))      Set prefix of all players in 'admin'
team_property('admin','display_name','Administrators')     Set display name for team 'admin'
team_property('admin','seeFriendlyInvisibles',true)       Make all players in 'admin' see other admins even when invisible
team_property('admin','deathMessageVisibility','hideForOtherTeams')       Make all players in 'admin' see other admins even when invisible
```

## `bossbar()`, `bossbar(id)`, `bossbar(id,property,value?)`

Manage bossbars just like with the `/bossbar` command.

Without any arguments, returns a list of all bossbars.

When an id is specified, creates a bossbar with that `id` and returns the id of the created bossbar.
Bossbar ids need a namespace and a name. If no namespace is specified, it will automatically use `minecraft:`.
In that case you should keep track of the bossbar with the id that `bossbar(id)` returns, because a namespace may be added automatically.
If the id was invalid (for example by having more than one colon), returns `null`.
If the bossbar already exists, returns `false`.

`bossbar('timer') => 'minecraft:timer'` (Adds the namespace `minecraft:` because none is specified)

`bossbar('scarpet:test') => 'scarpet:test'` In this case there is already a namespace specified

`bossbar('foo:bar:baz') => null` Invalid identifier

`bossbar(id,property)` is used to query the `property` of a bossbar.

`bossbar(id,property,value)` can modify the `property` of the bossbar to a specified `value`.

Available properties are:

* color: can be `'pink'`, `'blue'`, `'red'`, `'green'`, `'yellow'`, `'purple'` or `'white'`

* style: can be `'progress'`, `'notched_6'`, `'notched_10'`, `'notched_12'` or `'notched_20'`

* value: value of the bossbar progress

* max: maximum value of the bossbar progress, by default 100

* name: Text to display above the bossbar, supports formatted text

* visible: whether the bossbar is visible or not

* players: List of players that can see the bossbar

* add_player: add a player to the players that can see this bossbar, this can only be used for modifying (`value` must be present)

* remove: remove this bossbar, no `value` required

```
bossbar('script:test','style','notched_12')
bossbar('script:test','value',74)
bossbar('script:test','name',format('rb Test'))  -> Change text
bossbar('script:test','visible',false)  -> removes visibility, but keeps players
bossbar('script:test','players',player('all'))  -> Visible for all players
bossbar('script:test','players',player('Steve'))  -> Visible for Steve only 
bossbar('script:test','players',null)  -> Invalid player, removing all players
bossbar('script:test','add_player',player('Alex'))  -> Add Alex to the list of players that can see the bossbar
bossbar('script:test','remove')  -> remove bossbar 'script:test'
for(bossbar(),bossbar(_,'remove'))  -> remove all bossbars
```




