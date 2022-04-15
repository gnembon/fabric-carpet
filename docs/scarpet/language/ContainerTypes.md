# Lists, Maps and API support for Containers

Scarpet supports basic container types: lists and maps (aka hashmaps, dicts etc..)

## Container manipulation

Here is a list of operations that work on all types of containers: lists, maps, as well as other Minecraft specific 
modifyable containers, like NBTs

### `get(container, address, ...), get(lvalue), ':' operator`

Returns the value at `address` element from the `value`. For lists it indicates an index, use negative numbers to 
reach elements from the end of the list. `get` call will always be able to find the index. In case there is few 
items, it will loop over

for maps, retrieves the value under the key specified in the `address` or null otherwise

[Minecraft specific usecase]: In case `value` is of `nbt` type, uses address as the nbt path to query, returning null, 
if path is not found, one value if there was one match, or list of values if result is a list. Returned elements can 
be of numerical type, string texts, or another compound nbt tags

In case to simplify the access with nested objects, you can add chain of addresses to the arguments of `get` rather 
than calling it multiple times. `get(get(foo,a),b)` is equivalent to `get(foo, a, b)`, or `foo:a:b`.

<pre>
get([range(10)], 5)  => 5
get([range(10)], -1)  => 9
get([range(10)], 10)  => 0
[range(10)]:93  => 3
get(player() ~ 'nbt', 'Health') => 20 // inefficient way to get player health, use player() ~ 'health' instead
get({ 'foo' -> 2, 'bar' -> 3, 'baz' -> 4 }, 'bar')  => 3
</pre>

### `has(container, address, ...), has(lvalue)`

Similar to `get`, but returns boolean value indicating if the given index / key / path is in the container. 
Can be used to determine if `get(...)==null` means the element doesn't exist, or the stored value for this 
address is `null`, and is cheaper to run than `get`.

Like get, it can accept multiple addresses for chains in nested containers. In this case `has(foo:a:b)` is 
equivalent to `has(get(foo,a), b)` or `has(foo, a, b)`

### `delete(container, address, ...), delete(lvalue)`

Removes specific entry from the container. For the lists - removes the element and shrinks it. For maps, it 
removes the key from the map, and for nbt - removes content from a given path.

Like with the `get` and `has`, `delete` can accept chained addresses, as well as l-value container access, removing 
the value from the leaf of the path provided, so `delete(foo, a, b)` is the 
same as `delete(get(foo,a),b)` or `delete(foo:a:b)`

Returns true, if container was changed, false, if it was left unchanged, and null if operation was invalid.

### `put(container, address, value), put(container, address, value, mode), put(lvalue, value)`

<u>**Lists**</u>

Modifies the container by replacing the value under the address with the supplied `value`. For lists, a valid 
index is required, but can be negative as well to indicate positions from the end of the list. If `null` is 
supplied as the address, it always means - add to the end of the list.

There are three modes that lists can have items added to them:

*   `replace`(default): Replaces item under given index(address). Doesn't change the size of the array 
unless `null` address is used, which is an exception and then it appends to the end
*   `insert`: Inserts given element at a specified index, shifting the rest of the array to make space for the item. 
Note that index of -1 points to the last element of the list, thus inserting at that position and moving the previous 
last element to the new last element position. To insert at the end, use `+=` operator, or `null` address in put
*   `extend`: treats the supplied value as an iterable set of values to insert at a given index, extending the list 
by this amount of items. Again use `null` address/index to point to the end of the list

Due to the extra mode parameter, there is no chaining for `put`, but you can still use l-value container access to 
indicate container and address, so `put(foo, key, value)` is the same as `put(foo:key, value)` or `foo:key=value`

Returns true, if container got modified, false otherwise, and null if operation was invalid.

<u>**Maps**</u>

For maps there are no modes available (yet, seems there is no reason to). It replaces the value under the supplied 
key (address), or sets it if not currently present.

<u>**NBT Tags**</u>

The address for nbt values is a valid nbt path that you would use with `/data` command, and tag is any tag that 
would be applicable for a given insert operation. Note that to distinguish between proper types (like integer types, 
you need to use command notation, i.e. regular ints is `123`, while byte size int would be `123b` and an explicit 
string would be `"5"`, so it helps that scarpet uses single quotes in his strings. Unlike for lists and maps, it 
returns the number of affected nodes, or 0 if none were affected.

There are three modes that NBT tags can have items added to them:

*   `replace`(default): Replaces item under given path(address). Removes them first if possible, and then adds given 
element to the supplied position. The target path can indicate compound tag keys, lists, or individual elements 
of the lists.
*   `<N>`: Index for list insertions. Inserts given element at a specified index, inside a list specified with the 
path address. Fails if list is not specified. It behaves like `insert` mode for lists, i.e. it is not removing any 
of the existing elements. Use `replace` to remove and replace existing element.
*   `merge`: assumes that both path and replacement target are of compound type (dictionaries, maps, `{}` types), 
and merges keys from `value` with the compound tag under the path

<pre>
a = [1, 2, 3]; put(a, 1, 4); a  => [1, 4, 3]
a = [1, 2, 3]; put(a, null, 4); a  => [1, 2, 3, 4]
a = [1, 2, 3]; put(a, 1, 4, 'insert'); a  => [1, 4, 2, 3]
a = [1, 2, 3]; put(a, null, [4, 5, 6], 'extend'); a  => [1, 2, 3, 4, 5, 6]
a = [1, 2, 3]; put(a, 1, [4, 5, 6], 'extend'); a  => [1, 4, 5, 6, 2, 3]
a = [[0,0,0],[0,0,0],[0,0,0]]; put(a:1, 1, 1); a  => [[0, 0, 0], [0, 1, 0], [0, 0, 0]]
a = {1,2,3,4}; put(a, 5, null); a  => {1: null, 2: null, 3: null, 4: null, 5: null}
tag = nbt('{}'); put(tag, 'BlockData.Properties', '[1,2,3,4]'); tag  => {BlockData:{Properties:[1,2,3,4]}}
tag = nbt('{a:[{lvl:3},{lvl:5},{lvl:2}]}'); put(tag, 'a[].lvl', 1); tag  => {a:[{lvl:1},{lvl:1},{lvl:1}]}
tag = nbt('{a:[{lvl:[1,2,3]},{lvl:[3,2,1]},{lvl:[4,5,6]}]}'); put(tag, 'a[].lvl', 1, 2); tag
     => {a:[{lvl:[1,2,1,3]},{lvl:[3,2,1,1]},{lvl:[4,5,1,6]}]}
tag = nbt('{a:[{lvl:[1,2,3]},{lvl:[3,2,1]},{lvl:[4,5,6]}]}'); put(tag, 'a[].lvl[1]', 1); tag
     => {a:[{lvl:[1,1,3]},{lvl:[3,1,1]},{lvl:[4,1,6]}]}
</pre>

## List operations

### `[value, ...?]`,`[iterator]`,`l(value, ...?)`, `l(iterator)`

Creates a list of values of the expressions passed as parameters. It can be used as an L-value and if all 
elements are variables, you coujld use it to return multiple results from one function call, if that 
function returns a list of results with the same size as the `[]` call uses. In case there is only one 
argument and it is an iterator (vanilla expression specification has `range`, but Minecraft API implements 
a bunch of them, like `diamond`), it will convert it to a proper list. Iterators can only be used in high order 
functions, and are treated as empty lists, unless unrolled with `[]`. 

Internally, `[elem, ...]`(list syntax) and `l(elem, ...)`(function syntax) are equivalent. `[]` is simply translated to 
`l()` in the scarpet preprocessing stage. This means that internally the code has always expression syntax despite `[]`
not using different kinds of brackets and not being proper operators. This means that `l(]` and `[)` are also valid
although not recommended as they will make your code far less readable. 

<pre>
l(1,2,'foo') <=> [1, 2, 'foo']
l() <=> [] (empty list)
[range(10)] => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
[1, 2] = [3, 4] => Error: l is not a variable
[foo, bar] = [3, 4]; foo==3 && bar==4 => 1
[foo, bar, baz] = [2, 4, 6]; [min(foo, bar), baz] = [3, 5]; [foo, bar, baz]  => [3, 4, 5]
</pre>

In the last example `[min(foo, bar), baz]` creates a valid L-value, as `min(foo, bar)` finds the lower of the 
variables (in this case `foo`) creating a valid assignable L-list of `[foo, baz]`, and these values 
will be assigned new values

### `join(delim, list), join(delim, values ...)`

Returns a string that contains joined elements of the list, iterator, or all values, 
concatenated with `delim` delimiter

<pre>
join('-',range(10))  => 0-1-2-3-4-5-6-7-8-9
join('-','foo')  => foo
join('-', 'foo', 'bar')  => foo-bar
</pre>

### `split(delim?, expr)`

Splits a string under `expr` by `delim` which can be a regular expression. If no delimiter is specified, it splits
by characters.

If `expr` is a list, it will split the list into multiple sublists by the element (s) which equal `delim`, or which equal the empty string
in case no delimiter is specified.

Splitting a `null` value will return an empty list.

<pre>
split('foo') => [f, o, o]
split('','foo')  => [f, o, o]
split('.','foo.bar')  => []
split('\\.','foo.bar')  => [foo, bar]
split(1,[2,5,1,2,3,1,5,6]) => [[2,5],[2,3],[5,6]]
split(1,[1,2,3,1,4,5,1] => [[], [2,3], [4,5], []]
split(null) => []
</pre>

### `slice(expr, from, to?)`

extracts a substring, or sublist (based on the type of the result of the expression under expr with 
starting index of `from`, and ending at `to` if provided, or the end, if omitted. Can use negative indices to 
indicate counting form the back of the list, so `-1 <=> length(_)`.

Special case is made for iterators (`range`, `rect` etc), which does require non-negative indices (negative `from` is treated as
`0`, and negative `to` as `inf`), but allows retrieving parts of the sequence and ignore
other parts. In that case consecutive calls to `slice` will refer to index `0` the current iteration position since iterators
cannot go back nor track where they are in the sequence (see examples).

<pre>
slice([0,1,2,3,4,5], 1, 3)  => [1, 2]
slice('foobar', 0, 1)  => 'f'
slice('foobar', 3)  => 'bar'
slice(range(10), 3, 5)  => [3, 4]
slice(range(10), 5)  => [5, 6, 7, 8, 9]
r = range(100); [slice(r, 5, 7), slice(r, 1, 3)]  => [[5, 6], [8, 9]]
</pre>

### `sort(list), sort(values ...)`

Sorts in the default sortographical order either all arguments, or a list if its the only argument. 
It returns a new sorted list, not affecting the list passed to the argument

<pre>sort(3,2,1)  => [1, 2, 3]
sort('a',3,11,1)  => [1, 3, 11, 'a']
list = [4,3,2,1]; sort(list)  => [1, 2, 3, 4]
</pre>

### `sort_key(list, key_expr)`

Sorts a copy of the list in the order or keys as defined by the `key_expr` for each element

<pre>
sort_key([1,3,2],_)  => [1, 2, 3]
sort_key([1,3,2],-_)  => [3, 2, 1]
sort_key([range(10)],rand(1))  => [1, 0, 9, 6, 8, 2, 4, 5, 7, 3]
sort_key([range(20)],str(_))  => [0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9]
</pre>

### `range(to), range(from, to), range(from, to, step)`

Creates a range of numbers from `from`, no greater/larger than `to`. The `step` parameter dictates not only the 
increment size, but also direction (can be negative). The returned value is not a proper list, just the iterator 
but if for whatever reason you need a proper list with all items evaluated, use `[range(to)]`. 
Primarily to be used in higher order functions

<pre>
range(10)  => [...]
[range(10)]  => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
map(range(10),_*_)  => [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
reduce(range(10),_a+_, 0)  => 45
range(5,10)  => [5, 6, 7, 8, 9]
range(20, 10, -2)  => [20, 18, 16, 14, 12]
range(-0.3, 0.3, 0.1)  => [-0.3, -0.2, -0.1, 0, 0.1, 0.2]
range(0.3, -0.3, -0.1) => [0.3, 0.2, 0.1, -0, -0.1, -0.2]
</pre>

## Map operations

Scarpet supports map structures, aka hashmaps, dicts etc. Map structure can also be used, with `null` values as sets. 
Apart from container access functions, (`. , get, put, has, delete`), the following functions:

### `{values, ...}`,`{iterator}`,`{key -> value, ...}`,`m(values, ...)`, `m(iterator)`, `m(l(key, value), ...))`

creates and initializes a map with supplied keys, and values. If the arguments contains a flat list, these are all 
treated as keys with no value, same goes with the iterator - creates a map that behaves like a set. If the 
arguments is a list of lists, they have to have two elements each, and then first is a key, and second, a value

In map creation context (directly inside `{}` or `m{}` call), `->` operator acts like a pair constructor for simpler
syntax providing key value pairs, so the invocation to `{foo -> bar, baz -> quux}` is equivalent to
`{[foo, bar], [baz, quux]}`, which is equivalent to somewhat older, but more traditional functional form of
`m(l(foo, bar),l(baz, quuz))`.

Internally, `{?}`(list syntax) and `m(?)`(function syntax) are equivalent. `{}` is simply translated to 
`m()` in the scarpet preprocessing stage. This means that internally the code has always expression syntax despite `{}`
not using different kinds of brackets and not being proper operators. This means that `m(}` and `{)` are also valid
although not recommended as they will make your code far less readable. 

When converting map value to string, `':'` is used as a key-value separator due to tentative compatibility with NBT
notation, meaning in simpler cases maps can be converted to NBT parsable string by calling `str()`. This however
does not guarantee a parsable output. To properly convert to NBT value, use `encode_nbt()`.

<pre>
{1, 2, 'foo'} => {1: null, 2: null, foo: null}
m() <=> {} (empty map)
{range(10)} => {0: null, 1: null, 2: null, 3: null, 4: null, 5: null, 6: null, 7: null, 8: null, 9: null}
m(l(1, 2), l(3, 4)) <=> {1 -> 2, 3 -> 4} => {1: 2, 3: 4}
reduce(range(10), put(_a, _, _*_); _a, {})
     => {0: 0, 1: 1, 2: 4, 3: 9, 4: 16, 5: 25, 6: 36, 7: 49, 8: 64, 9: 81}
</pre>

### `keys(map), values(map), pairs(map)`

Returns full lists of keys, values and key-value pairs (2-element lists) for all the entries in the map
