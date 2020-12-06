# Iterating over larger areas of blocks

These functions help scan larger areas of blocks without using generic loop functions, like nested `loop`.

### `scan(center, range, upper_range?, expr)`

Evaluates expression over area of blocks defined by its center `center = (cx, cy, cz)`, expanded in all directions 
by `range = (dx, dy, dz)` blocks, or optionally in negative with `range` coords, and `upper_range` coords in 
positive values, so you can use that if you know the lower coord, and dimension by calling `'scan(center, 0, 0, 0, w, h, d, ...)`.

`center` can be defined either as three coordinates, a single tuple of three coords, or a block value.
`range` and `upper_range` can have the same representations, just if they are block values, it computes the distance to the center
as range instead of taking the values as is.

`expr` receives `_x, _y, _z` as coords of current analyzed block and `_`, which represents the block itself.

Returns number of successful evaluations of `expr` (with `true` boolean result) unless called in void context, 
which would cause the expression not be evaluated for their boolean value.

`scan` also handles `continue` and `break` statements, using `continue`'s return value to use in place of expression
return value. `break` return value has no effect.

### `volume(from_pos, to_pos, expr)`

Evaluates expression for each block in the area, the same as the `scan` function, but using two opposite corners of 
the rectangular cuboid. Any corners can be specified, its like you would do with `/fill` command.
You can use a position or three coordinates to specify, it doesn't matter.

For return value and handling `break` and `continue` statements, see `scan` function above.

### `neighbours(pos)`

Returns the list of 6 neighbouring blocks to the argument. Commonly used with other loop functions like `for`.

<pre>
for(neighbours(x,y,z),air(_)) => 4 // number of air blocks around a block
</pre>

### `rect(centre, range?, upper_range?)`

Returns an iterator, just like `range` function that iterates over a rectangular area of blocks. If only center
point is specified, it iterates over 27 blocks. If `range` arguments are specified, expands selection by the  respective 
number of blocks in each direction. If `positive_range` arguments are specified,
 it uses `range` for negative offset, and `positive_range` for positive.

`centre` can be defined either as three coordinates, a list of three coords, or a block value.
`range` and `positive_range` can have the same representations, just if they are block values, it computes the distance to the center
as range instead of taking the values as is.`

### `diamond(centre_pos, radius?, height?)`

Iterates over a diamond like area of blocks. With no radius and height, its 7 blocks centered around the middle 
(block + neighbours). With a radius specified, it expands shape on x and z coords, and with a custom height, on y. 
Any of these can be zero as well. radius of 0 makes a stick, height of 0 makes a diamond shape pad.
