# Scoreboard

### `scoreboard()`, `scoreboard(objective)`, `scoreboard(objective, key)`, `scoreboard(objective, key, value)`

Displays or modifies individual scoreboard values. With no arguments, returns the list of current objectives.
With specified `objective`, lists all keys (players) associated with current objective. With specified `objective`,
`key`, returns current value of the objective for a given player (key). With additional `value` sets a new scoreboard
 value, returning previous value associated with the `key`.
 
### `scoreboard_add(objective, criterion?)`

Adds a new objective to scoreboard. If `criterion` is not specified, assumes `'dummy'`. Returns `false` if objective 
already existed, `true` otherwise.

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

sets display location for a specified `objective`. If `objective` is `null`, then display is cleared.

# Team

### `team_list()`, `team_list(team)`

Returns all available teams as a list with no arguments.

When a `team` is specified, it returns all the players inside that team. If the `team` is invalid, returns `null`.

### `team_add(team)`, `team_add(team,player)`

With one argument, creates a new `team` and returns its name if successfull, or `null` if team already exists.


`team_add('admin')` -> Create a team with the name 'admin'
`team_add('admin','Steve')` -> Joing the player 'Steve' into the team 'admin'

If a `player` is specified, the player will join the given `team`. Returns `true` if player joined the team, or `false` if nothing changed since the player was already in this team. If the team is invalid, returns `null`

### `team_remove(team)`

Removes a `team`. Returns `true` if the team was deleted, or `null` if the team is invalid.

### `team_leave(player)`

Removes the `player` from the team he is in. Returns `true` if the player left a team, otherwise `false`.

`team_leave('Steve')` -> Removes Steve from the team he is currently in

### `team_empty(team)`

Removes all players inside the `team` and returns the number of people that were in the team, or `null` if the team is invalid.

### `team_property(team,property,value?)`

Reads the `property` of the `team` if no `value` is specified. If a `value` is added as a third argument, it sets the `property` to that `value`.

The properties are the same as in `/team modify` command:

* `collisionRule`
  * Type: String
  * Options: always, never, pushOtherTeams, pushOwnTeam
    
* `color`
  * Type: String
  * Options: See [team command](https://minecraft.gamepedia.com/Commands/team#Arguments) (same strings as `'teamcolor'` [command argument](https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/Full.md#command-argument-types] options))

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
team_property('admin','displayName','Administrators')     Set display name for team 'admin'
team_property('admin','seeFriendlyInvisibles',true)       Make all players in 'admin' see other admins even when invisible
```

