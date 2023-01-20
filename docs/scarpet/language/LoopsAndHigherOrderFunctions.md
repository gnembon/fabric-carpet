# Loops, and higher order functions

Efficient use of these functions can greatly simplify your programs and speed them up, as these functions will 
internalize most of the operations that need to be applied on multiple values at the same time. Most of them take 
a `list` argument which can be any iterable structure in scarpet, including generators, like `rect`, or `range`, 
and maps, where the iterator returns all the map keys

## Loops

### `break(), break(expr), continue(), continue(expr)`

These allow to control execution of a loop either skipping current iteration code, using `continue`, or finishing the 
current loop, using `break`. `break` and `continue` can only be used inside `for`, `c_for`, `while`, `loop`, `map`,
`filter`, `reduce` as well as Minecraft API block loops, `scan` and `volume` 
functions, while `break` can be used in `first` as well. Outside of the internal expressions of these functions, 
calling `break` or `continue` will cause an error. In case of the nested loops, and more complex setups, use 
custom `try` and `throw` setup.

Please check corresponding loop function description what `continue` and `break` do in their contexts, but in 
general case, passed values to `break` and `continue` will be used in place of the return value of the internal 
iteration expression.

### `c_for(init, condition, increment, body)`

`c_for` Mimics c-style tri-arg (plus body) for loops. Return value of `c_for` is number of iterations performed in the
 loop. Unlike other loops, the `body` is not provided with pre-initialized `_` style variables - all initialization
 and increments has to be handled by the programmers themselves.
 `break` and `continue` statements are handled within `body` expression only, and not in `condition` or `increment`.
 
 <pre>
 c_for(x=0, x<10, x+=1,
    c_for(y=0, y<10, y+=1,
        print(str('%d * %d = %d', x, y, x*y))
    )
 )
 </pre> 

### `for(list,expr(_,_i))`

Evaluates expression over list of items from the `list`. Supplies `_`(value) and `_i`(iteration number) to the `expr`.

Returns the number of times `expr` was successful. Uses `continue` and `break` argument in place of the returned 
value from the `expr`(if supplied), to determine if the iteration was successful.

<pre>
check_prime(n) -> !first( range(2, sqrt(n)+1), !(n % _) );
for(range(1000000,1100000),check_prime(_))  => 7216
</pre>

From which we can learn that there is 7216 primes between 1M and 1.1M

### `while(cond, limit, expr)`

Evaluates expression `expr` repeatedly until condition `cond` becomes false, but not more than `limit` times. 
Returns the result of the last `expr` evaluation, or `null` if nothing was successful. Both `expr` and `cond` will 
received a bound variable `_` indicating current iteration, so its a number.

<pre>
while(a<100,10,a=_*_)  => 81 // loop exhausted via limit
while(a<100,20,a=_*_)  => 100 // loop stopped at condition, but a has already been assigned
while(_*_<100,20,a=_*_)  => 81 // loop stopped at condition, before a was assigned a value
</pre>

### `loop(num,expr(_),exit(_)?)`

Evaluates expression `expr`, `num` number of times. code>expr receives `_` system variable indicating the iteration.

<pre>
loop(5, game_tick())  => repeat tick 5 times
list = []; loop(5, x = _; loop(5, list += [x, _] ) ); list
  // double loop, produces: [[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [1, 0], [1, 1], ... , [4, 2], [4, 3], [4, 4]]
</pre>

In this small example we will search for first 10 primes, apparently including 0:

<pre>
check_prime(n) -> !first( range(2, sqrt(n)+1), !(n % _) );
primes = [];
loop(10000, if(check_prime(_), primes += _ ; if (length(primes) >= 10, break())));
primes
// outputs: [0, 1, 2, 3, 5, 7, 11, 13, 17, 19]
</pre>

## Higher Order Functions

### `map(list,expr(_,_i))`

Converts a `list` of values, to another list where each value is result of an expression `v = expr(_, _i)` 
where `_` is passed as each element of the list, and `_i` is the index of such element. If `break` is called the 
map returns whatever collected thus far. If `continue` and `break` are used with supplied argument, it is used in 
place of the resulting map element, otherwise current element is skipped.

<pre>
map(range(10), _*_)  => [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
map(player('*'), _+' is stoopid') [gnembon is stoopid, herobrine is stoopid]
</pre>

### `filter(list,expr(_,_i))`

filters `list` elements returning only these that return positive result of the `expr`. With `break` and `continue` 
statements, the supplied value can be used as a boolean check instead.

<pre>
filter(range(100), !(_%5), _*_>1000)  => [0, 5, 10, 15, 20, 25, 30]
map(filter(entity_list('*'),_=='Witch'), query(_,'pos') )  => [[1082.5, 57, 1243.5]]
</pre>

### `first(list,expr(_,_i))`

Finds and returns the first item in the list that satisfies `expr`. It sets `_` for current element value, 
and `_i` for index of that element. `break` can be called inside the iteration code, using its argument value 
instead of the current item. `continue` has no sense and cannot be called inside `first` call.

<pre>
first(range(1000,10000), n=_; !first( range(2, sqrt(n)+1), !(n % _) ) )  => 1009 // first prime after 1000
</pre>

Notice in the example above, that we needed to rename the outer `_` to be albe to use in in the inner `first` call

### `all(list,expr(_,_i))`

Returns `true` if all elements on the list satisfy the condition. Its roughly equivalent 
to `all(list,expr) <=> for(list,expr)==length(list)`. `expr` also receives bound `_` and `_i` variables. `break` 
and `continue` have no sense and cannot be used inside of `expr` body.

<pre>
all([1,2,3], check_prime(_))  => true
all(neighbours(x,y,z), _=='stone')  => true // if all neighbours of [x, y, z] are stone
map(filter(rect(0,4,0,1000,0,1000), [x,y,z]=pos(_); all(rect(x,y,z,1,0,1),_=='bedrock') ), pos(_) )
  => [[-298, 4, -703], [-287, 4, -156], [-269, 4, 104], [242, 4, 250], [-159, 4, 335], [-208, 4, 416], [-510, 4, 546], [376, 4, 806]]
    // find all 3x3 bedrock structures in the top bedrock layer
map( filter( rect(0,4,0,1000,1,1000,1000,0,1000), [x,y,z]=pos(_);
        all(rect(x,y,z,1,0,1),_=='bedrock') && for(rect(x,y-1,z,1,1,1,1,0,1),_=='bedrock')<8),
   pos(_) )  => [[343, 3, -642], [153, 3, -285], [674, 3, 167], [-710, 3, 398]]
    // ditto, but requiring at most 7 bedrock block in the 18 blocks below them
</pre>

### `reduce(list,expr(_a,_,_i), initial)`

Applies `expr` for each element of the list and saves the result in `_a` accumulator. Consecutive calls to `expr` 
can access that value to apply more values. You also need to specify the initial value to apply for the 
accumulator. `break` can be used to terminate reduction prematurely. If a value is provided to `break` or `continue`, 
it will be used from now on as a new value for the accumulator.

<pre>
reduce([1,2,3,4],_a+_,0)  => 10
reduce([1,2,3,4],_a*_,1)  => 24
</pre>

