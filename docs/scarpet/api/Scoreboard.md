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

### `scoreboard_remove(objective)`

Removes an objective. Returns `true` if objective has existed and has been removed.

### `scoreboard_display(place, objective)`

sets display location for a specified `objective`. If `place` is `null`, then display is cleared.
