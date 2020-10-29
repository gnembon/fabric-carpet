__command()->(
    print(player(),format('wb Draw command'));
    print(player(),'Enables /draw command, for drawing simple shapes');
    print(player(),format('g or other shapes which are sorta difficult to do normally'));
    ''
);

//Command funcs

diamond_shape(cx,cy,cz,radius,block)->(

    pos = [cx, cy, cz];

    c_for(r = 0, r < radius, r+=1,
    
        y=r-radius+1;
        c_for (x = -r, x <= r, x+=1,
            z=r-abs(x);
            affected+= __setBlock([pos:0+x, pos:1-y, pos:2+z], block);
            affected+= __setBlock([pos:0+x, pos:1-y, pos:2-z], block);
            affected+= __setBlock([pos:0+x, pos:1+y, pos:2+z], block);
            affected+= __setBlock([pos:0+x, pos:1+y, pos:2-z], block);
        )
    );
    print(player(), str(' Filled %s blocks',affected));
);

___diamond_shape()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'block'->'block'
};

cone(cx, cy, cz, radius, height, pointup, orientation, block, hollow)->
    __drawPyramid(cx, cy, cz, radius, height, pointup, orientation, block, 'circle', hollow);

___cone()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'height'->'number',
    'pointup'->'bool',
    'orientation'->'string',
    'block'->'block',
    'hollow'->'bool'
};

pyramid(cx, cy, cz, radius, height, pointup, orientation, block, hollow)->
    __drawPyramid(cx, cy, cz, radius, height, pointup, orientation, block, 'square', hollow);

___pyramid()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'height'->'number',
    'pointup'->'bool',
    'orientation'->'string',
    'block'->'block',
    'hollow'->'bool'
};

cylinder(cx, cy, cz, radius, height, orientation, block, hollow)->
    __drawPrism(cx, cy, cz, radius, height, orientation, block, 'circle', hollow);

___cylinder()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'height'->'number',
    'orientation'->'string',
    'block'->'block',
    'hollow'->'bool'
};

cuboid(cx, cy, cz, radius, height, orientation, block, hollow)->
    __drawPrism(cx, cy, cz, radius, height, orientation, block, 'square', hollow);

___cuboid()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'height'->'number',
    'orientation'->'string',
    'block'->'block',
    'hollow'->'bool'
};

sphere(cx, cy, cz, radius, block)->
    __drawSphere(cx, cy, cz, radius, block, false);

___sphere()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'block'->'block',
    'hollow'->'bool'
};

ball(cx, cy, cz, radius, block)->
    __drawSphere(cx, cy, cz, radius, block, true);

___ball()->{
    'cx'->'number',
    'cy'->'number',
    'cz'->'number',
    'radius'->'number',
    'block'->'block',
    'hollow'->'bool'
};

//Extra funcs

__setBlock(pos,block)->(
    if(block(pos)!=block,
        set(pos,block);
        return(1),
        return(0)
    )
);

__lengthSq(x, y, z)-> return ((x * x) + (y * y) + (z * z));

__fillFlat(pos, offset, radius, rectangle, orientation, block, hollow)->(


    successes=0;
    r = floor(radius);
    drsq = radius*radius;
    if (orientation=='x',
        c_for(a=-r, a<=r, a+=1,
            c_for(b=-r, b<=r, b+=1, 
                if(rectangle && (!hollow || (abs(a) == r || abs(b) ==r)) || !rectangle && (a*a + b*b <= drsq && (!hollow || (abs(a)+1)*(abs(a)+1) + (abs(b)+1)*(abs(b)+1) >= drsq)),
                    successes += __setBlock([pos:0+offset, pos:1+a, pos:2+b], block)
                )
            )   
        ),
        orientation=='y',
        c_for(a=-r, a<=r, a+=1,
            c_for(b=-r, b<=r, b+=1, 
                if(rectangle && (!hollow || (abs(a) == r || abs(b) ==r)) || !rectangle && (a*a + b*b <= drsq && (!hollow || (abs(a)+1)*(abs(a)+1) + (abs(b)+1)*(abs(b)+1) >= drsq)),
                    successes += __setBlock([pos:0+a, pos:1+offset, pos:2+b], block)
                )
            )   
        ),
        orientation=='z',
        c_for(a=-r, a<=r, a+=1,
                if(rectangle && (!hollow || (abs(a) == r || abs(b) ==r)) || !rectangle && (a*a + b*b <= drsq && (!hollow || (abs(a)+1)*(abs(a)+1) + (abs(b)+1)*(abs(b)+1) >= drsq)), 
                if(rectangle || a*a + b*b <= drsq,
                    successes += __setBlock([pos:0+b, pos:1+a, pos:2+offset], block)
                )
            )   
        )
    );
    return (successes);
);

__drawPyramid(cx, cy, cz, radius, height, pointup, orientation, block, base, hollow)->(

    pos = [cx, cy, cz];

    affected = 0;
    
    isSquare = if(base=='square',true,false);

    c_for(i=0, i<height,i+=1,

        r = if(pointup, radius - radius * i / height - 1, radius * i / height);
        affected+= __fillFlat(pos , i, r, isSquare, str(orientation), block, if(i!=0,hollow))

    );
    
    print(player(), 'Filled ' + affected + ' blocks')
);

__drawPrism(cx, cy, cz, radius, height, orientation, block, base, hollow)->(
        
    pos = [cx, cy, cz];

    affected = 0;
    
    isSquare = if(base=='square',true,false);

    c_for(i=0, i<height,i+=1,
        affected+= __fillFlat(pos , i, radius, isSquare, str(orientation), block, if(i!=0&&i!=height-1,hollow))
    );
    
    print(player(), 'Filled ' + affected + ' blocks')
);

__drawSphere(cx, cy, cz, radius, block, solid)->(
    pos = [cx, cy, cz];

    affected = 0;
    
    radiusX = radius+0.5;
    radiusY = radius+0.5;
    radiusZ = radius+0.5;

    nextXn = 0;

    c_for (x = 0, x <= ceil(radiusX), x+=1,
    
        xn = nextXn;
        nextXn = (x + 1) / radiusX;
        nextYn = 0;

        c_for (y = 0, y <= ceil(radiusY), y+=1,
        
            yn = nextYn;
            nextYn = (y + 1) / radiusY;
            nextZn = 0;

            c_for (z = 0, z <= ceil(radiusZ), z+=1,
            
                zn = nextZn;
                nextZn = (z + 1) / radiusZ;

                distanceSq = __lengthSq(xn, yn, zn);
                if (distanceSq > 1, break());

                if (!solid && __lengthSq(nextXn, yn, zn) <= 1 && __lengthSq(xn, nextYn, zn) <= 1 && __lengthSq(xn, yn, nextZn) <= 1,
                    continue()
                );

                c_for (xmod = -1, xmod < 2, xmod += 2,
                
                    c_for (ymod = -1, ymod < 2, ymod += 2,
                    
                        c_for (zmod = -1, zmod < 2, zmod += 2,

                            affected+= __setBlock(pos+[xmod * x, ymod * y, zmod * z],block);
                        )
                    )
                )
            )
        )
    );

    print(player(), 'Filled ' + affected + ' blocks')
)