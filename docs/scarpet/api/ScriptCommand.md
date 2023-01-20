# `/script run` command

Primary way to input commands. The command executes in the context, position, and dimension of the executing player, 
commandblock, etc... The command receives 4 variables, `x`, `y`, `z` and `p` indicating position and 
the executing entity of the command. You will receive tab completion suggestions as you type your code suggesting 
functions and global variables. It is advisable to use `/execute in ... at ... as ... run script run ...` or similar, 
to simulate running commands in a different scope.

# `/script load / unload <app> (global?)`, `/script in <app>` commands

`load / unload` commands allow for very convenient way of writing your code, providing it to the game and 
distribute with your worlds without the need of use of commandblocks. Just place your Scarpet code in the 
`/scripts` folder of your world files and make sure it ends with `.sc` extension. In singleplayer, you can 
also save your scripts in `.minecraft/config/carpet/scripts` to make them available in any world.

The good thing about editing that code is that you can not only use normal editing without the need of marking of newlines,  
but you can also use comments in your code.

A comment is anything that starts with a double slash, and continues to the end of the line:

<pre>
foo = 1;
//This is a comment
bar = 2;
// This never worked, so I commented it out
// baz = foo()
</pre>

### `/script load/unload <app> (?global)`

Loading operation will load that script code from disk and execute it right away. You would probably use it to load 
some stored procedures to be used for later. To reload the module, just type `/script load` again. Reloading removes 
all the current global state (globals and functions) that were added later by the module. To reload all apps along with 
all game resources, use vanilla `/reload` command.



Loaded apps have the ability to store and load external files, especially their persistent tag state. For that 
check `load_app_data` and `store_app_data` functions.



Unloading the app will only mask their command tree, not remove it. This has the same effect than not having that command
at all, with the exception that if you load a different app with the same name, this may cause commands to reappear.
To remove the commands fully, use `/reload`.



### `/script in <app> ...`

Allows to run normal /script commands in a specific app, like `run, invoke,..., globals` etc...

# `/script invoke / invokepoint / invokearea`, `/script globals` commands

`invoke` family of commands provide convenient way to invoke stored procedures (i.e. functions that has been 
defined previously by any running script. To view current stored procedure set, 
run `/script globals`(or `/script globals all` to display all functions even hidden ones), to define a new stored 
procedure, just run a `/script run function(a,b) -> ( ... )` command with your procedure once, and to forget a 
procedure, use `undef` function: `/script run undef('function')`

### `/script invoke <fun> <args?> ...`

Equivalent of running `/script run fun(args, ...)`, but you get the benefit of getting the tab completion of the 
command name, and lower permission level required to run these (since player is not capable of running any custom 
code in this case, only this that has been executed before by an operator). Arguments will be checked for validity, 
and you can only pass simple values as arguments (strings, numbers, or `null` value). Use quotes to include 
whitespaces in argument strings.

Command will check provided arguments with required arguments (count) and fail if not enough or too much 
arguments are provided. Operators defining functions are advised to use descriptive arguments names, as these 
will be visible for invokers and form the base of understanding what each argument does.

`invoke` family of commands will tab complete any stored function that does not start with `'_'`, it will still 
allow to run procedures starting with `'_'` but not suggest them, and ban execution of any hidden stored procedures, 
so ones that start with `'__'`. In case operator needs to use subroutines for convenience and don't want to expose 
them to the `invoke` callers, they can use this mechanic.

<pre>
/script run example_function(const, phrase, price) -> print(const+' '+phrase+' '+price)
/script invoke example_function pi costs 5
</pre>

### `/script invokepoint <fun> <coords x y z> <args?> ...`

It is equivalent to `invoke` except it assumes that the first three arguments are coordinates, and provides 
coordinates tab completion, with `looking at...` mechanics for convenience. All other arguments are expected 
at the end

### `/script invokearea <fun> <coords x y z> <coords x y z> <args?> ...`

It is equivalent to `invoke` except it assumes that the first three arguments are one set of coordinates, 
followed by the second set of coordinates, providing tab completion, with `looking at...` mechanics for convenience, 
followed by any other required arguments

# `/script scan`, `/script fill` and `/script outline` commands

These commands can be used to evaluate an expression over an area of blocks. They all need to have specified the 
origin of the analyzed area (which is used as referenced (0,0,0), and two corners of an area to analyzed. If you 
would want that the script block coordinates refer to the actual world coordinates, use origin of `0 0 0`, or if 
it doesn't matter, duplicating coordinates of one of the corners is the easiest way.

These commands, unlike raw `/script run` are limited by vanilla fill / clone command limit of 32k blocks, which can 
be altered with carpet mod's own `/carpet fillLimit` command.

### `/script scan origin<x y z> corner<x y z> corner<x y z> expr`

Evaluates expression for each point in the area and returns number of successes (result was positive). Since the 
command by itself doesn't affect the area, the effects would be in side effects.

### `/script fill origin<x y z> corner<x y z> corner<x y z> "expr" <block> (? replace <replacement>)`

Think of it as a regular fill command, that sets blocks based on whether a result of the command was successful. 
Note that the expression is in quotes. Thankfully string constants in `scarpet` use single quotes. Can be used to 
fill complex geometric shapes.

### `/script outline origin<x y z> corner<x y z> corner<x y z> "expr" <block> (? replace <replacement>)`

Similar to `fill` command it evaluates an expression for each block in the area, but in this case setting blocks 
where condition was true and any of the neighbouring blocks were evaluated negatively. This allows to create surface 
areas, like sphere for example, without resorting to various rounding modes and tricks.

Here is an example of seven ways to draw a sphere of radius of 32 blocks around 0 100 0:

<pre>
/script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z <  32*32" white_stained_glass replace air
/script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z <= 32*32" white_stained_glass replace air
/script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z <  32.5*32.5" white_stained_glass replace air
/script fill    0 100 0 -40 60 -40 40 140 40 "floor(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
/script fill    0 100 0 -40 60 -40 40 140 40 "round(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
/script fill    0 100 0 -40 60 -40 40 140 40 "ceil(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
/draw sphere 0 100 0 32 white_stained_glass replace air // fluffy ball round(sqrt(x*x+y*y+z*z)-rand(abs(y)))==32
</pre>

The last method is the one that world edit is using (part of carpet mod). It turns out that the outline method 
with `32.5` radius, fill method with `round` function and draw command are equivalent

# `script stop/script resume` command

`/script stop` allows to stop execution of any script currently running that calls the `game_tick()` function which 
allows the game loop to regain control of the game and process other commands. This will also make sure that all 
current and future programs will stop their execution. Execution of all programs will be prevented 
until `/script resume` command is called.

Lets look at the following example. This is a program computes Fibonacci number in a recursive manner:

<pre>
fib(n) -> if(n<3, 1, fib(n-1)+fib(n-2) ); fib(8)
</pre>

That's really bad way of doing it, because the higher number we need to compute the compute requirements will 
rise exponentially with `n`. It takes a little over 50 milliseconds to do fib(24), so above one tick, but about 
a minute to do fib(40). Calling fib(40) will not only freeze the game, but also you woudn't be able to interrupt 
its execution. We can modify the script as follows

<pre>
fib(n) -> ( game_tick(50); if(n<3, 1, fib(n-1)+fib(n-2) ) ); fib(40)
</pre>

But this would never finish as such call would finish after `~ 2^40` ticks. To make our computations responsive, 
yet able to respond to user interactions, other commands, as well as interrupt execution, we could do the following:

<pre>
fib(n) -> ( if(n==23, game_tick(50) ); if(n<3, 1, fib(n-1)+fib(n-2) ) ); fib(40)
</pre>

This would slow down the computation of fib(40) from a minute to two, but allows the game to keep continue running 
and be responsive to commands, using about half of each tick to advance the computation. Obviously depending on the 
problem, and available hardware, certain things can take more or less time to execute, so portioning of work with 
calling `gametick` should be balanced in each case separately

# `/script download` command

`/script download <path>` command allows downloading and running apps directly from an online app store (it's all free), 
by default the [scarpet app store](https://www.github.com/gnembon/scarpet).
Downloaded apps will be placed in the world's scripts folder automatically. Location of the app store is controlled
with a global carpet setting of `/carpet scriptsAppStore`. Apps, if required, will also download all the resources they need
to run it. Consecutive downloads of the same app will re-download its content and its resources, but will not remove anything
that has been removed or renamed.

# `/script remove` command

command allow to stop and remove apps installed in the worlds scripts folder. The app is unloaded and app 'sc' file is moved
to the `/scripts/trash`. Removed apps can only be restored by manually moving it back from the trash folder,
or by redownloading from the appstore.
