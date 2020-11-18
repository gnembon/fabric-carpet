import('math','_euclidean_sq','_vec_length');

__config() -> {
    'commands'->{
        'sphere <center> <radius> <block>'->['draw_sphere',null, false],
        'sphere <center> <radius> <block> replace <replacement>'->['draw_sphere',false],
        'ball <center> <radius> <block>'->['draw_sphere', null, true],
        'ball <center> <radius> <block> replace <replacement>'->['draw_sphere', true],
        'diamond <center> <radius> <block>'->['draw_diamond',null],
        'diamond <center> <radius> <block> replace <replacement>'->'draw_diamond',
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->['draw_pyramid',null, false],
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->['draw_pyramid', false],
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->['draw_pyramid',null, true],
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->['draw_pyramid', true],
        'cylinder <center> <radius> <height> <orientation> <block> <hollow>'->['draw_prism', null, false],
        'cylinder <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->['draw_prism', false],
        'cuboid <center> <radius> <height> <orientation> <block> <hollow>'->['draw_prism', null, true],
        'cuboid <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->['draw_prism', true]
    },
    'arguments'->{
        'center'->{'type'->'pos', 'loaded'->'true'},
        'radius'->{'type'->'int', 'suggest'->[], 'min'->0},//to avoid default suggestions
        'replacement'->{'type'->'block'},
        'height'->{'type'->'int', 'suggest'->[],'min'->0},
        'orientation'->{'type'->'term', 'suggest'->['x','y','z']},
        'pointing'->{'type'->'term','suggest'->['up','down']},
        'hollow'->{'type'->'bool'}
    },
    'scope'->'global'
};



test(a,b,c)->print(center+ ' ' + radius + ' ' + block);

//"Boilerplate" code

set_block(x, y, z, block, replacement)-> (
    if(block != block(x, y, z),
        set([x, y, z],block);
        global_affected += 1;
    );
);

global_affected = 0;

affected(player) -> (
    print(player,format('gi Filled ' + global_affected + ' blocks'));
    affected = global_affected;
    global_affected = 0;
    affected
);

length_sq(vec)->return(_vec_length(vec)^2);//cos of lengthSq func in DrawCommand.java

fill_flat(pos, offset, dr, rectangle, orientation, block, hollow, replacement)->(
    r = floor(dr);
    drsq = dr^2;
    if(orientation=='x',
        c_for(a=-r,a<=r,a+=1,
            c_for(b=-r,b<=r,b+=1,
                if((!hollow && (rectangle || a*a + b*b <= drsq))||//if not hollow, vry simple
                    (hollow && ((rectangle && (abs(a) == r || abs(b) ==r)) || //If hollow and it's a rectangle
                    (!rectangle && (a*a + b*b <= drsq && (abs(a)+1)^ 2 + (abs(b)+1)^2 >= drsq)))),//If hollow and not rectangle
                    set_block(pos:0+offset,pos:1+a,pos:2+b,block, replacement)
                )
            )
        )
    ,orientation == 'y',
        c_for(a=-r,a<=r,a+=1,
            c_for(b=-r,b<=r,b+=1,
                if((!hollow && (rectangle || a*a + b*b <= drsq))||//if not hollow, vry simple
                    (hollow && ((rectangle && (abs(a) == r || abs(b) ==r)) || //If hollow and it's a rectangle
                    (!rectangle && (a*a + b*b <= drsq && (abs(a)+1)^ 2 + (abs(b)+1)^2 >= drsq)))),//If hollow and not rectangle
                    set_block(pos:0+a,pos:1+offset,pos:2+b,block, replacement)
                )
            )
        )
    ,orientation == 'z',
        c_for(a=-r,a<=r,a+=1,
            c_for(b=-r,b<=r,b+=1,
                if((!hollow && (rectangle || a*a + b*b <= drsq))||//if not hollow, vry simple
                    (hollow && ((rectangle && (abs(a) == r || abs(b) ==r)) || //If hollow and it's a rectangle
                    (!rectangle && (a*a + b*b <= drsq && (abs(a)+1)^2 + (abs(b)+1)^2 >= drsq)))),//If hollow and not rectangle
                    set_block(pos:0+b,pos:1+a,pos:2+offset,block, replacement)
                )
            )
        )
    ,
        print(player(),format('r Error while running command: orientation can only be "x", "y" or "z", '+orientation+' is invalid.'));
        global_affected = 0;
    );
);

//drawing commands
draw_sphere(pos, radius, block, replacement, hollow)->(

    radiusX = radius+0.5;
    radiusY = radius+0.5;
    radiusZ = radius+0.5;

    ceilRadiusX = ceil(radiusX);
    ceilRadiusY = ceil(radiusY);
    ceilRadiusZ = ceil(radiusZ);

    nextXn = 0;

    c_for(x=0,x<=ceilRadiusX,x+=1,
        xn=nextXn;
        nextXn = (x+1)/radiusX;
        nextYn = 0;
        c_for(y=0,y<=ceilRadiusY,y+=1,
            yn = nextYn;
            nextYn = (y + 1) / radiusY;
            nextZn = 0;
            instaquit = false;
            c_for(z=0,z<=ceilRadiusZ,z+=1,
                if(!instaquit,
                    zn = nextZn;
                    nextZn = (z+1)/radiusZ;

                    if(length_sq([xn, yn, zn]) > 1,instaquit = true);

                    if(!(!hollow && length_sq([nextXn, yn, zn]) <= 1 && length_sq([xn, nextYn, zn]) <= 1 && length_sq([xn, yn, nextZn]) <= 1),
                        c_for(xmod=-1,xmod<2,xmod+=2,
                            c_for(ymod=-1,ymod<2,ymod+=2,
                                c_for(zmod=-1,zmod<2,zmod+=2,
                                    set_block(pos:0 + xmod*x, pos:1 + ymod*y, pos:2 + zmod*z, block, replacement)
                                )
                            )
                        )
                    )
                )
            )
        )
    );

    affected(player())
);


draw_diamond(pos, radius, block, replacement)->(

    c_for(r=0, r<radius, r+=1,
        y = r-radius+1;
        c_for(x=-r,x<=r,x+=1,
            z=r-abs(x);

            set_block(pos:0+x,pos:1-y,pos:2+z, block, replacement);
            set_block(pos:0+x,pos:1-y,pos:2-z, block, replacement);
            set_block(pos:0+x,pos:1+y,pos:2+z, block, replacement);
            set_block(pos:0+x,pos:1+y,pos:2-z, block, replacement);
        )
    );

    affected(player())
);

draw_pyramid(pos, rad, height, pointing, orientation, block, hollow, replacement, is_square)->(

    pointup = pointing=='up';
    radius = rad+0.5;

    for(range(height),
        r = if(pointup, radius - radius * _ / height, radius * _ / height);
        fill_flat(pos, _, r, is_square, orientation, block, if((pointup&&_==0)||(!pointup && _==height-1),false,hollow),replacement)//Always close bottom off
    );
    affected(player())
);

draw_prism(pos, rad, height, orientation, block, hollow, replacement, is_square)->(
    radius = rad+0.5;

    for(range(height),
        fill_flat(pos, _, radius, is_square, orientation, block, if(_==0 || _==height-1,false,hollow), replacement)//Always close ends off
    );
    affected(player())
);

//then(literal("cylinder").
//        then(argument("center", BlockPosArgumentType.blockPos()).
//                then(argument("radius", IntegerArgumentType.integer(1)).
//                        then(argument("height",IntegerArgumentType.integer(1)).
//                                        then(argument("orientation",StringArgumentType.word()).suggests( (c, b) -> suggestMatching(new String[]{"y","x","z"},b))
//                                                .then(drawShape(c -> DrawCommand.drawPrism(c, "circle")))))))).
//then(literal("cuboid").
//        then(argument("center", BlockPosArgumentType.blockPos()).
//                then(argument("radius", IntegerArgumentType.integer(1)).
//                        then(argument("height",IntegerArgumentType.integer(1)).
//                                then(argument("orientation",StringArgumentType.word()).suggests( (c, b) -> suggestMatching(new String[]{"y","x","z"},b))
//                                        .then(drawShape(c -> DrawCommand.drawPrism(c, "square"))))))));
