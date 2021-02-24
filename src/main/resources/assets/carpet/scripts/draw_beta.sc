import('math','_euclidean_sq','_vec_length');

__config() -> {
    'commands'->{
        'sphere <center> <radius> <block>'->_(c,r,b)->draw('draw_sphere',[c,r,b,null,true]),
        'sphere <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_sphere',[c,r,b,rp,true]),
        'ball <center> <radius> <block>'->_(c,r,b)->draw('draw_sphere',[c,r,b,null,false]),
        'ball <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_sphere',[c,r,b,rp,false]),
        'diamond <center> <radius> <block> hollow' -> _(c,r,b)->draw('draw_diamond',[c,r,b,null]),
        'diamond <center> <radius> <block> hollow replace <replacement>'->_(c,r,b,rp)->draw('draw_diamond',[c, r, b, rp]),
        'diamond <center> <radius> <block>' -> _(c,r,b)->draw('draw_filled_diamond',[c,r,b,null]),
        'diamond <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_filled_diamond',[c, r, b, rp]),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->_(c,r,h,p,o,b,ho)->draw('draw_pyramid',[c,r,h,p,o,b,ho,null, true]),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,p,o,b,ho,rp)->draw('draw_pyramid', [c,r,h,p,o,b,ho,rp,true]),
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->_(c,r,h,p,o,b,ho)->draw('draw_pyramid',[c,r,h,p,o,b,ho,null, false]),
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,p,o,b,ho,rp)->draw('draw_pyramid', [c,r,h,p,o,b,ho,rp,false]),
        'cuboid <center> <radius> <height> <orientation> <block> <hollow>'->_(c,r,h,o,b,ho)->draw('draw_prism',[c,r,h,p,o,b,ho,null, true]),
        'cuboid <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,o,b,ho,rp)->draw('draw_prism', [c,r,h,o,b,ho,rp,true]),
        'cylinder <center> <radius> <height> <orientation> <block> <hollow>'->_(c,r,h,o,b,ho)->draw('draw_prism',[c,r,h,p,o,b,ho,null, false]),
        'cylinder <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,o,b,ho,rp)->draw('draw_prism', [c,r,h,o,b,ho,rp,false]),
        if(system_info('server_dev_environment')||system_info('world_carpet_rules'):'superSecretSetting',
            'debug <bool>'->_(bool)->global_debug=bool,
            ''->_()->''
        )
    },
    'arguments'->{
        'center'->{'type'->'pos', 'loaded'->'true'},
        'radius'->{'type'->'int', 'suggest'->[], 'min'->0},//to avoid default suggestions
        'replacement'->{'type'->'blockpredicate'},
        'height'->{'type'->'int', 'suggest'->[],'min'->0},
        'orientation'->{'type'->'term', 'suggest'->['x','y','z']},
        'pointing'->{'type'->'term','suggest'->['up','down']},
        'hollow'->{'type'->'term','suggest'->['hollow','solid']},
        'mode'->{'type'->'term','options'->['never', 'ingame', 'always']}
    },
    'scope'->'global'
};

global_debug=false;

//"Boilerplate" code


_block_matches(existing, block_predicate) ->
(
    [name, block_tag, properties, nbt] = block_predicate;

    (name == null || name == existing) &&
    (block_tag == null || block_tags(existing, block_tag)) &&
    all(properties, block_state(existing, _) == properties:_) &&
    (!tag || tag_matches(block_data(existing), tag))
);

set_block(x, y, z, block, replacement)->//doing it like this so I can get list of blocks to place from functions
    global_positions:0+=[[x, y, z], block, replacement];//Cos optional args return list always


global_positions = [{}, 0];//using a set so I dont waste compute on duplicate positions

affected(player) -> (
    affected = global_positions;
    print(player,format('gi Filled ' + affected:1 + ' blocks'));
    global_positions = [{},0];
    affected
);

length_sq(vec) -> reduce(vec, _a + _*_, 0);

fill_flat(pos, offset, dr, rectangle, orientation, block, hollow, replacement)->(
    r = floor(dr);
    drsq = dr^2;
    if(orientation=='x',
        scan(pos,0,-r,-r,
            if((!hollow && (rectangle || _y^2 + _z^2 <= drsq))||//if not hollow, vry simple
                (hollow && ((rectangle && (abs(_y) == r || abs(_z) ==r)) || //If hollow and it's a rectangle
                (!rectangle && (_y^2 + _z^2 <= drsq && (abs(_y)+1)^ 2 + (abs(_z)+1)^2 >= drsq)))),//If hollow and not rectangle
                set_block(_x+offset,_y,_z,block, replacement)
            )
        ),
    orientation == 'y',
        scan(pos,-r,0,-r,
            if((!hollow && (rectangle || _x^2 + _z^2 <= drsq))||//if not hollow, vry simple
                (hollow && ((rectangle && (abs(_x) == r || abs(_z) ==r)) || //If hollow and it's a rectangle
                (!rectangle && (_x^2 + _z^2 <= drsq && (abs(_x)+1)^ 2 + (abs(_z)+1)^2 >= drsq)))),//If hollow and not rectangle
                set_block(_x,_y+offset,_z,block, replacement)
            )
        ),
    orientation == 'z',
        scan(pos,-r,-r,0,
            if((!hollow && (rectangle || _y^2 + _x^2 <= drsq))||//if not hollow, vry simple
                (hollow && ((rectangle && (abs(_y) == r || abs(_x) ==r)) || //If hollow and it's a rectangle
                (!rectangle && (_y^2 + _x^2 <= drsq && (abs(_y)+1)^ 2 + (abs(_x)+1)^2 >= drsq)))),//If hollow and not rectangle
                set_block(_x,_y,_z+offset,block, replacement)
            )
        )
    )
);

//Drawing commands

draw(what, args)->(//custom setter cos it's easier
    call(what,args);//putting blocks to be set into the first element of global_positions

    for(global_positions:0,
        [pos, block, replacement]=_;
        existing = block(pos);
        if(block != existing && (!replacement || _block_matches(existing, replacement) ),
            global_positions:1 += bool(set(existing,block))
        )
    );

    affected(player());
);

draw_sphere(args)->(
    if(global_debug, start_time=unix_time());

    [centre, radius, block, replacement, hollow] = args;

    [cx,cy,cz]=centre;
    for(range(-90, 90, 45/radius),
        cpitch = cos(_);
        spitch = sin(_);
        for(range(0, 180, 45/radius),
            cyaw = cos(_)*cpitch*radius;
            syaw = sin(_)*cpitch*radius;
            if(hollow,
                set_block(cx+cyaw,cy+spitch*radius,cz+syaw,block,replacement);
                set_block(cx+cos(_+180)*cpitch*radius,cy+spitch*radius,cz+sin(_+180)*cpitch*radius,block,replacement),
                for(range(-syaw,syaw+1),
                    set_block(cx+cyaw*cpitch,cy+spitch*radius,cz+_,block,replacement)
                )
            )
        )
    );

    if(global_debug,
        print(player(),format('gi Time taken: '+(unix_time()-start_time)+'ms'))
    );
    return(global_positions)
);

draw_diamond(args)->(
    if(global_debug, start_time=unix_time());

    [pos, radius, block, replacement] = args;

    c_for(r=0, r<radius, r+=1,
        y = r-radius+1;
        c_for(x=-r,x<=r,x+=1,
            z=r-abs(x);

            set_block(pos:0+x, pos:1 +y, pos:2+z, block, replacement);
            set_block(pos:0+x, pos:1 +y, pos:2-z, block, replacement);
            set_block(pos:0+x, pos:1 -y, pos:2+z, block, replacement);
            set_block(pos:0+x, pos:1 -y, pos:2-z, block, replacement);
        )
    );

    if(global_debug,
        print(player(),format('gi Time taken: '+(unix_time()-start_time)+'ms'))
    );
    return(global_positions)
);

draw_filled_diamond(args)->(
    if(global_debug, start_time=unix_time());

    [pos, radius, block, replacement] = args;

    for(diamond(pos,radius,radius),
        set_block(_x,_y,_z,block,replacement)
    );

    if(global_debug,
        print(player(),format('gi Time taken: '+(unix_time()-start_time)+'ms'))
    );
    return(global_positions)
);

draw_pyramid(args)->(
    if(global_debug, start_time=unix_time());

    [pos, radius, height, pointing, orientation, block, fill_type, replacement, is_square] = args;

    hollow = fill_type=='hollow';
    pointup = pointing=='up';
    for(range(height),
        r = if(pointup, radius * ( 1- _ / height) -1, radius * _ / height);
        fill_flat(pos, _, r, is_square, orientation, block, if((pointup&&_==0)||(!pointup && _==height-1),false,hollow),replacement)//Always close bottom off
    );

    if(global_debug,
        print(player(),format('gi Time taken: '+(unix_time()-start_time)+'ms'))
    );
    return(global_positions)
);

draw_prism(args)->(
    if(global_debug, start_time=unix_time());

    [pos, rad, height, orientation, block, fill_type, replacement, is_square]=args;

    hollow = fill_type =='hollow';
    radius = rad+0.5;

    for(range(height),
        fill_flat(pos, _, radius, is_square, orientation, block, if(_==0 || _==height-1,false,hollow), replacement)//Always close ends off
    );

    if(global_debug,
        print(player(),format('gi Time taken: '+(unix_time()-start_time)+'ms'))
    );
    return(global_positions)
);