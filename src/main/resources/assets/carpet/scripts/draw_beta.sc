import('math','_euclidean_sq','_vec_length');

__config() -> {
    'commands'->{
        'sphere <center> <radius> <block>'->['draw_sphere',null,true],
        'sphere <center> <radius> <block> replace <replacement>'->['draw_sphere',true],
        'ball <center> <radius> <block>'->['draw_sphere',null,false],
        'ball <center> <radius> <block> replace <replacement>'->['draw_sphere',false],
        'diamond <center> <radius> <block>'->['draw_diamond',null],
        'diamond <center> <radius> <block> replace <replacement>'->'draw_diamond',
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->['draw_pyramid',null, true],
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->['draw_pyramid', true],
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->['draw_pyramid',null, false],
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->['draw_pyramid', false],
        'cuboid <center> <radius> <height> <orientation> <block> <hollow>'->['draw_prism', null, true],
        'cuboid <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->['draw_prism', true],
        'cylinder <center> <radius> <height> <orientation> <block> <hollow>'->['draw_prism', null, false],
        'cylinder <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->['draw_prism', false]
    },
    'arguments'->{
        'center'->{'type'->'pos', 'loaded'->'true'},
        'radius'->{'type'->'int', 'suggest'->[], 'min'->0},//to avoid default suggestions
        'replacement'->{'type'->'blockpredicate'},
        'height'->{'type'->'int', 'suggest'->[],'min'->0},
        'orientation'->{'type'->'term', 'suggest'->['x','y','z']},
        'pointing'->{'type'->'term','suggest'->['up','down']},
        'hollow'->{'type'->'term','suggest'->['hollow','solid']},
    },
    'scope'->'global'
};


//"Boilerplate" code

_block_matches(existing, block_predicate) ->
(
    [name, block_tag, properties, nbt] = block_predicate;

    (name == null || name == existing) &&
    (block_tag == null || block_tags(existing, block_tag)) &&
    all(properties, block_state(existing, _) == properties:_) &&
    (!tag || tag_matches(block_data(existing), tag))
);

set_block(x, y, z, block, replacement)-> (
    existing = block(x, y, z);
    if(block != existing && (replacement && _block_matches(existing, replacement) ),
        without_updates( if (set(existing,block),
            global_affected += [x, y, z];
        ));
    );
);

global_affected = [];

affected(player) -> (
    affected = length(global_affected);
    print(player,format('gi Filled ' + affected + ' blocks'));

    if(system_info('world_carpet_rules'):'fillUpdates',
        for(global_affected,update(_));//updating if fillUpdates is true
    );

    global_affected = [];
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
        ),
        print(player(),format('r Error while running command: orientation can only be "x", "y" or "z", '+orientation+' is invalid.'));
        global_affected = [];
    );
);

//drawing commands

draw_sphere(centre, radius, block, replacement, hollow)->(
    scan(centre,[radius,radius,radius],
        l = length_sq([_x,_y,_z]-centre);
        if((l<=radius^2+radius) && (!hollow || l>=radius^2-radius),
            set_block(_x, _y, _z, block, replacement)
        )
    );

    affected(player());
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

draw_pyramid(pos, radius, height, pointing, orientation, block, fill_type, replacement, is_square)->(

    hollow = fill_type=='hollow';
    pointup = pointing=='up';

    for(range(height),
        r = if(pointup, radius - radius * _ / height -1, radius * _ / height);
        fill_flat(pos, _, r, is_square, orientation, block, if((pointup&&_==0)||(!pointup && _==height-1),false,hollow),replacement)//Always close bottom off
    );
    affected(player())
);

draw_prism(pos, rad, height, orientation, block, fill_type, replacement, is_square)->(

    hollow = fill_type =='hollow';
    radius = rad+0.5;

    for(range(height),
        fill_flat(pos, _, radius, is_square, orientation, block, if(_==0 || _==height-1,false,hollow), replacement)//Always close ends off
    );
    affected(player())
);
