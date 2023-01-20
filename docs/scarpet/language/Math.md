# Arithmetic operations

## Basic Arithmetic Functions

There is bunch of them - they require a number and spit out a number, doing what you would expect them to do.

### `fact(n)`

Factorial of a number, a.k.a `n!`, just not in `scarpet`. Gets big... quick... Therefore, values larger 
than `fact(20)` will not return the exact value, but a value with 'double-float' precision.

### `sqrt(n)`

Square root (not 'a squirt') of a number. For other fancy roots, use `^`, math and yo noggin. Imagine square roots on a tree...

### `abs(n)`

Absolut value.

### `round(n)`

Closest integer value. Did you know the earth is also round?

### `floor(n)`

Highest integer that is still no larger then `n`. Insert a floor pun here.

### `ceil(n)`

First lucky integer that is not smaller than `n`. As you would expect, ceiling is typically right above the floor.

### `ln(n)`

Natural logarithm of `n`. Naturally.

### `ln1p(n)`

Natural logarithm of `n+1`. Very optimistic.

### `log10(n)`

Decimal logarithm of `n`. Its ceiling is the length of its floor.

### `log(n)`

Binary logarithm of `n`. Finally, a proper one, not like the previous 11.

### `log1p(n)`

Binary logarithm of `n+1`. Also always positive.

### `mandelbrot(a, b, limit)`

Computes the value of the mandelbrot set, for set `a` and `b` spot. Spot the beetle. Why not.

### `min(arg, ...), min(list), max(arg, ...), max(list)`

Compute minimum or maximum of supplied arguments assuming default sorthoraphical order. 
In case you are missing `argmax`, just use `a ~ max(a)`, little less efficient, but still fun.

Interesting bit - `min` and `max` don't remove variable associations from arguments, which means can be used as 
LHS of assignments (obvious case), or argument spec in function definitions (far less obvious).

<pre>
a = 1; b = 2; min(a,b) = 3; [a,b]  => [3, 2]
a = 1; b = 2; fun(x, min(a,b)) -> [a,b]; fun(3,5)  => [5, 0]
</pre>

Absolutely no idea, how the latter might be useful in practice. But since it compiles, can ship it.

### `relu(n)`

Linear rectifier of `n`. 0 below 0, n above. Why not. `max(0,n)` with less moral repercussions.

## Trigonometric / Geometric Functions

### `sin(x)`

### `cos(x)`

### `tan(x)`

### `asin(x)`

### `acos(x)`

### `atan(x)`

### `atan2(x,y)`

### `sinh(x)`

### `cosh(x)`

### `tanh(x)`

### `sec(x)`

### `csc(x)`

### `sech(x)`

### `csch(x)`

### `cot(x)`

### `acot(x)`

### `coth(x)`

### `asinh(x)`

### `acosh(x)`

### `atanh(x)`

### `rad(deg)`

### `deg(rad)`

Use as you wish
