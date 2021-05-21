# Fundamental components of `scarpet` programming language.

Scarpet (a.k.a. Carpet Script, or Script for Carpet) is a programming language 
designed to provide the ability to write custom programs to run within Minecraft 
and interact with the world.

This specification is divided into two sections: this one is agnostic to any 
Minecraft related features and could function on its own, and CarpetExpression 
for Minecraft specific routines and world manipulation functions.

# Synopsis

<pre>
script run print('Hello World!')
</pre>

or an OVERLY complex example:

<pre>
/script run
    block_check(x1, y1, z1, x2, y2, z2, block_to_check) ->
    (
        [minx, maxx] = sort([x1, x2]);
        [miny, maxy] = sort([y1, y2]);
        [minz, maxz] = sort([z1, z2]);
        'Need to compute the size of the area of course';
        'Cause this language doesn\'t support comments in the command mode';
        xsize = maxx - minx + 1;
        ysize = maxy - miny + 1;
        zsize = maxz - minz + 1;
        total_count = 0;
        loop(xsize,
            xx = minx + _ ;
            loop(ysize,
                yy = miny + _ ;
                loop(zsize,
                    zz = minz + _ ;
                    if ( block(xx,yy,zz) == block_to_check,
                        total_count += ceil(rand(1))
                    )
                )
            )
        );
        total_count
    );
    check_area_around_closest_player_for_block(block_to_check) ->
    (
        closest_player = player();
        [posx, posy, posz] = query(closest_player, 'pos');
        total_count = block_check( posx-8,1,posz-8, posx+8,17,posz+8, block_to_check);
        print('There is '+total_count+' of '+block_to_check+' around you')
    )
/script invoke check_area_around_closest_player_for_block 'diamond_ore'
</pre>

or simply

<pre>
/script run print('There is '+for(rect(x,9,z,8,8,8), _ == 'diamond_ore')+' diamond ore around you')
</pre>

It definitely pays to check what higher level `scarpet` functions have to offer.

# Programs

You can think of an program like a mathematical expression, like `"2.4*sin(45)/(2-4)"` or `"sin(y)>0 & max(z, 3)>3"`.
Writing a program, is like writing a `2+3`, just a bit longer.

## Basic language components

Programs consist of constants, like `2`, `3.14`, `pi`, or `'foo'`, operators like `+`, `/`, `->`, variables which you 
can define, like `foo` or special ones that will be defined for you, like `_x`, or `_` , which are specific to each
built in function, and functions with name, and arguments in the form of `f(a,b,c)`, where `f` is the function name,
and `a, b, c` are the arguments which can be any other expression. And that's all the parts of the language, so all
in all - sounds quite simple.

## Code flow

Like any other proper programming language, `scarpet` needs brackets, basically to identify where stuff begins and 
where it ends. In the languages that uses much more complicated constructs, like Java, they tend to use all sort of 
them, round ones to indicate function calls, curly to indicate section of code, square to access lists, pointy for 
generic types etc... I mean - there is no etc, cause they have exhausted all the bracket options...

`Scarpet` is different, since it runs everything based on functions (although its not per se a functional 
language like lisp) only needs the round brackets for everything, and it is up to the programmer to organize 
its code so its readable, as adding more brackets does not have any effect on the performance of the programs 
as they are compiled before they are executed. Look at the following example usage of `if()` function:

<pre>
if(x&lt;y+6,set(x,8+y,z,'air');plop(x,top('surface',x,z),z,'birch'),sin(query(player(),'yaw'))&gt;0.5,plop(0,0,0,'boulder'),particle('fire',x,y,z))
</pre>

Would you prefer to read

<pre>
if(   x&lt;y+6,
           set(x,8+y,z,'air');
           plop(x,top('surface',x,z),z,'birch'),
      sin(query(player(),'yaw'))>0.5,
           plop(0,0,0,'boulder'),
      particle('fire',x,y,z)
)
</pre>

Or rather:

<pre>
if
(   x&lt;y+6,
    (
        set(x,8+y,z,'air');
        plop(x,top('surface',x,z),z,'birch')
    ),
    // else if
    sin(query(player(),'yaw'))>0.5,
    (
        plop(0,0,0,'boulder')
    ),
    // else
    particle('fire',x,y,z)
)
</pre>

Whichever style you prefer it doesn't matter. It typically depends on the situation and the complexity of the 
subcomponents. No matter how many whitespaces and extra brackets you add - the code will evaluate to exactly the 
same expression, and will run exactly the same, so make sure your programs are nice and clean so others don't 
have problems with them

## Functions and scoping

Users can define functions in the form `fun(args....) -> expression` and they are compiled and saved for further 
execution in this, but also subsequent calls of /script command, added to events, etc. Functions can also be
 assigned to variables, 
passed as arguments, called with `call('fun', args...)` function, but in most cases you would want to 
call them directly by 
name, in the form of `fun(args...)`. This means that once defined functions are saved with the world for 
further use. For variables, there are two types of them, global - which are shared anywhere in the code, 
and those are all which name starts with 'global_', and local variables which is everything else and those 
are only visible inside each function. This also means that all the parameters in functions are 
passed 'by value', not 'by reference'.

## Outer variables

Functions can still 'borrow' variables from the outer scope, by adding them to the function signature wrapped 
around built-in function `outer`. It adds the specified value to the function call stack so they behave exactly 
like capturing lambdas in Java, but unlike java captured variables don't need to be final. Scarpet will just 
attach their new values at the time of the function definition, even if they change later. Most value will be 
copied, but mutable values, like maps or lists, allow to keep the 'state' with the function, allowing them to 
have memory and act like objects so to speak. Check `outer(var)` for details.

## Code delivery, line indicators

Note that this should only apply to pasting your code to execute with commandblock. Scarpet recommends placing 
your code in apps (files with `.sc` extension that can be placed inside `/scripts` folder in the world files 
or as a globally available app in singleplayer in the `.minecraft/config/carpet/scripts` folder and loaded 
as a Scarpet app with the command `/script load [app_name]`. Scarpet apps loaded from disk should only 
contain code, no need to start with `/script run` prefix.

The following is the code that could be provided in a `foo.sc` app file located in world `/scripts` folder

<pre>
run_program() -> (
  loop( 10,
    // looping 10 times
    // comments are allowed in scripts located in world files
    // since we can tell where that line ends
    foo = floor(rand(10));
    check_not_zero(foo);
    print(_+' - foo: '+foo);
    print('  reciprocal: '+  _/foo )
  )
);
check_not_zero(foo) -> (
  if (foo==0, foo = 1)
)
</pre>

Which we then call in-game with:

<pre>
/script load foo
/script in foo invoke run_program
</pre>

However the following code can also be input as a command, or in a command block.

Since the maximum command that can be input to the chat is limited in length, you will be probably inserting your 
programs by pasting them to command blocks or reading from world files, however pasting to command blocks will 
remove some whitespaces and squish your newlines making the code not readable. If you are pasting a program that 
is perfect and will never cause an error, I salute you, but for the most part it is quite likely that your program 
might break, either at compile time, when its initially analyzed, or at execute time, when you suddenly attempt to 
divide something by zero. In these cases you would want to get a meaningful error message, but for that you would 
need to indicate for the compiler where did you put these new lines, since command block would squish them. For that, 
place at the beginning of the line to let the compiler know where are you. This makes so that `$` is the only 
character that is illegal in programs, since it will be replaced with new lines. As far as I know, `$` is not 
used anywhere inside Minecraft identifiers, so this shouldn't hinder the abilities of your programs.

Consider the following program executed as command block command:

<pre>
/script run
run_program() -> (
  loop( 10,
    foo = floor(rand(_));
    check_not_zero(foo);
    print(_+' - foo: '+foo);
    print('  reciprocal: '+  _/foo )
  )
);
check_not_zero(foo) -> (
   if (foo==0, foo = 1)
)
</pre>

Lets say that the intention was to check if the bar is zero and prevent division by zero in print, but because 
the `foo` is passed as a variable, it never changes the original foo value. Because of the inevitable division 
by zero, we get the following message:

<pre>
Your math is wrong, Incorrect number format for NaN at pos 98
run_program() -> ( loop( 10, foo = floor(rand(_)); check_not_zero(foo); print(_+' - foo: '+foo);
HERE>> print(' reciprocal: '+ _/foo ) ));check_not_zero(foo) -> ( if (foo==0, foo = 1))
</pre>

As we can see, we got our problem where the result of the mathematical operation was not a number 
(infinity, so not a number), however by pasting our program into the command made it squish the newlines so 
while it is clear where the error happened and we still can track the error down, the position of the error (98) 
is not very helpful and wouldn't be useful if the program gets significantly longer. To combat this issue we can 
precede every line of the script with dollar signs `$`:

<pre>
/script run
$run_program() -> (
$  loop( 10,
$    foo = floor(rand(_));
$    check_not_zero(foo);
$    print(_+' - foo: '+foo);
$    print('  reciprocal: '+  _/foo )
$  )
$);
$check_not_zero(foo) -> (
$   if (foo==0, foo = 1)
$)
</pre>

Then we get the following error message

<pre>
Your math is wrong, Incorrect number format for NaN at line 7, pos 2
  print(_+' - foo: '+foo);
   HERE>> print(' reciprocal: '+ _/foo )
  )
</pre>

As we can note not only we get much more concise snippet, but also information about the line number and position, 
so means its way easier to locate the potential problems problem

Obviously that's not the way we intended this program to work. To get it `foo` modified via a function call, 
we would either return it as a result and assign it to the new variable:

<pre>
foo = check_not_zero(foo);
...
check_not_zero(foo) -> if(foo == 0, 1, foo)
</pre>

.. or convert it to a global variable, which in this case passing as an argument is not required

<pre>
global_foo = floor(rand(10));
check_foo_not_zero();
...
check_foo_not_zero() -> if(global_foo == 0, global_foo = 1)
</pre>

## Scarpet preprocessor

There are several preprocessing operations applied to the source of your program to clean it up and prepare for
execution. Some of them will affect your code as it is reported via stack traces and function definition, and some
are applied only on the surface.
 - stripping `//` comments (in file mode)
 - replacing `$` with newlines (in command mode, modifies submitted code)
 - removing extra semicolons that don't follow `;` use as a binary operator, allowing for lenient use of semicolons
 - translating `{` into `m(`, `[` into `l(`, and `]` and `}` into `)`
 
No further optimizations are currently applied to your code.

## Mentions

LR1 parser, tokenizer, and several built-in functions are built based on the EvalEx project.
EvalEx is a handy expression evaluator for Java, that allows to evaluate 
simple mathematical and boolean expressions. EvalEx is distributed under MIT licence.
For more information, see: [EvalEx GitHub repository](https://github.com/uklimaschewski/EvalEx)
