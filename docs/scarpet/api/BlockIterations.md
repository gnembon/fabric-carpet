# Iterating over larger areas of blocks

These functions help scan larger areas of blocks without using generic loop functions, like nested `loop`.

### `scan(cx, cy, cz, dx, dy, dz, px?, py?, pz?, expr)`, `scan(center, range, lower_range?, expr)`

Evaluates expression over area of blocks defined by its center `center = (cx, cy, cz)`, expanded in all directions 
by `range = (dx, dy, dz)` blocks, or optionally in negative with `range` coords, and `upper_range` coords in 
positive values.
`center` can be defined either as three coordinates, a list of three coords, or a block value.
`range` and `lower_range` can have the same representations, just if its a block, it computes the distance to the center
as range instead of taking the values as is.
`expr` receives `_x, _y, _z` as coords of current analyzed block and `_`, which represents the block itself.

Returns number of successful evaluations of `expr` (with `true` boolean result) unless called in void context, 
which would cause the expression not be evaluated for their boolean value.

`scan` also handles `continue` and `break` statements, using `continue`'s return value to use in place of expression
return value. `break` return value has no effect.

### `volume(x1, y1, z1, x2, y2, z2, expr), volume(pos1, pos2, expr)`

Evaluates expression for each block in the area, the same as the `scan` function, but using two opposite corners of 
the rectangular cuboid. Any corners can be specified, its like you would do with `/fill` command.
You can use a position or three coordinates to specify, it doesn't matter.

For return value and handling `break` and `continue` statements, see `scan` function above.

### `neighbours(x, y, z), neighbours(block), neighbours(l(x,y,z))`

Returns the list of 6 neighbouring blocks to the argument. Commonly used with other loop functions like `for`.

<pre>
for(neighbours(x,y,z),air(_)) => 4 // number of air blocks around a block
</pre>

### `rect(cx, cy, cz, dx?, dy?, dz?, px?, py?, pz?), rect(centre, difference?, positive_diff?)`

Returns an iterator, just like `range` function that iterates over a rectangular area of blocks. If only center
point is specified, it iterates over 27 blocks. If `d` (`difference`) arguments are specified, expands selection by the  respective 
number of blocks in each direction. If `p` (`positive_diff`) arguments are specified, it uses `d` (`difference`) for negative offset, and `p` for positive.

`centre` can be defined either as three coordinates, a list of three coords, or a block value.
`difference` and `positive_diff` can have the same representations, just if its a block, it computes the distance to the center
as range instead of taking the values as is.`

### `diamond(cx, cy, cz, radius?, height?), diamond(centre, radius?, height?)`

Iterates over a diamond like area of blocks. With no radius and height, its 7 blocks centered around the middle 
(block + neighbours). With a radius specified, it expands shape on x and z coords, and with a custom height, on y. 
Any of these can be zero as well. radius of 0 makes a stick, height of 0 makes a diamond shape pad.
