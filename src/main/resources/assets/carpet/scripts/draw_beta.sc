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

global_shape_cache={//Saving shapes so dont have to recalculate them again and again
    'sphere'->{},//using <String,Array<Pos>> where the positions are relative to centre. (just using java notation cos its clearer)
    'diamond'->{},//eg: {'radius'->whatever,'height'->bla,'orientation'->bla} gets mapped to the vectors to place blocks in from the centre.
    'pyramid'->{},//so then it's more efficient to place same shape over and over again.
    'prism'->{}//using strings for storage cos can't use maps as keys
};

_block_matches(existing, block_predicate) ->
(
    [name, block_tag, properties, nbt] = block_predicate;

    (name == null || name == existing) &&
    (block_tag == null || block_tags(existing, block_tag)) &&
    all(properties, block_state(existing, _) == properties:_) &&
    (!tag || tag_matches(block_data(existing), tag))
);

set_block(x, y, z, block, replacement, cpos)-> (
    existing = block(x, y, z);
    if(block != existing && (!replacement || _block_matches(existing, replacement) ),
        global_positions:1 += bool(set(existing,block));
    );
    global_positions:0+=[x, y ,z]-cpos//so it's relative to centre pos, and we can draw from cache more smartly
);

global_positions = [[],0];

affected(player) -> (
    affected = global_positions:1;
    print(player,format('gi Filled ' + affected + ' blocks'));

    global_positions = [[],0];
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
                set_block(_x+offset,_y,_z,block, replacement, pos)
            )
        ),
    orientation == 'y',
        scan(pos,-r,0,-r,
            if((!hollow && (rectangle || _x^2 + _z^2 <= drsq))||//if not hollow, vry simple
                (hollow && ((rectangle && (abs(_x) == r || abs(_z) ==r)) || //If hollow and it's a rectangle
                (!rectangle && (_x^2 + _z^2 <= drsq && (abs(_x)+1)^ 2 + (abs(_z)+1)^2 >= drsq)))),//If hollow and not rectangle
                set_block(_x,_y+offset,_z,block, replacement, pos)
            )
        ),
    orientation == 'z',
        scan(pos,-r,-r,0,
            if((!hollow && (rectangle || _y^2 + _x^2 <= drsq))||//if not hollow, vry simple
                (hollow && ((rectangle && (abs(_y) == r || abs(_x) ==r)) || //If hollow and it's a rectangle
                (!rectangle && (_y^2 + _x^2 <= drsq && (abs(_y)+1)^ 2 + (abs(_x)+1)^2 >= drsq)))),//If hollow and not rectangle
                set_block(_x,_y,_z+offset,block, replacement, pos)
            )
        )
    );
);

draw_from_cache(cache, args_key, pos, block, replacement)->(
    if(!has(global_shape_cache:cache,str(args_key)),print('not in cache');return(false));
    print('drawn form cache');

    positions = global_shape_cache:cache:str(args_key);
    for(positions,
        set_block(_:0+pos:0, _:1+pos:1, _:2+pos:2, block, replacement, pos)
    )
);

save_to_cache(cache, args_key)->(
    put(global_shape_cache:cache, {str(args_key)->global_positions:0)};
    print('Saved '+args_key+' to cache')
);

//drawing commands

draw_sphere(centre, radius, block, replacement, hollow)->(
    if(!draw_from_cache('sphere',{
                'radius'->radius,
                'hollow'->hollow
            }, centre, block, replacement
            ),

        scan(centre,[radius,radius,radius],
            l = length_sq([_x,_y,_z]-centre);
            if((l<=radius^2+radius) && (!hollow || l>=radius^2-radius),
                set_block(_x, _y, _z, block, replacement, centre)
            )
        );
        save_to_cache('sphere',{
            'radius'->radius,
            'hollow'->hollow
        })
    );
    affected(player());
);

draw_diamond(pos, radius, block, replacement)->(
    if(!draw_from_cache('diamond',{'radius'->radius}, pos, block, replacement),
        c_for(r=0, r<radius, r+=1,
            y = r-radius+1;
            c_for(x=-r,x<=r,x+=1,
                z=r-abs(x);

                set_block(pos:0+x,pos:1-y,pos:2+z, block, replacement, pos);
                set_block(pos:0+x,pos:1-y,pos:2-z, block, replacement, pos);
                set_block(pos:0+x,pos:1+y,pos:2+z, block, replacement, pos);
                set_block(pos:0+x,pos:1+y,pos:2-z, block, replacement, pos);
            )
        );
        save_to_cache('diamond',{'radius'->radius})
    );
    affected(player())
);

draw_pyramid(pos, radius, height, pointing, orientation, block, fill_type, replacement, is_square)->(
    cache = {//this one's longer cos of a ton of params
        'radius'->radius,
        'height'->height,
        'pointing'->pointing,
        'orientation'->orientation,
        'fill_type'->fill_type,
        'is_square'->is_square
    };
    if(!draw_from_cache('pyramid',cache , pos, block, replacement),
        hollow = fill_type=='hollow';
        pointup = pointing=='up';

        for(range(height),
            r = if(pointup, radius - radius * _ / height -1, radius * _ / height);
            fill_flat(pos, _, r, is_square, orientation, block, if((pointup&&_==0)||(!pointup && _==height-1),false,hollow),replacement)//Always close bottom off
        );
        save_to_cache('pyramid',{
            'radius'->radius,
            'height'->height,
            'pointing'->pointing,
            'orientation'->orientation,
            'fill_type'->fill_type,
            'is_square'->is_square
        })
    );
    affected(player())
);

draw_prism(pos, rad, height, orientation, block, fill_type, replacement, is_square)->(
    cache = {
        'radius'->radius,
        'height'->height,
        'orientation'->orientation,
        'fill_type'->fill_type,
        'is_square'->is_square
    };
    if(!draw_from_cache('prism',cache , pos, block, replacement),
        hollow = fill_type =='hollow';
        radius = rad+0.5;

        for(range(height),
            fill_flat(pos, _, radius, is_square, orientation, block, if(_==0 || _==height-1,false,hollow), replacement)//Always close ends off
        );
        save_to_cache('pyramid',cache)
    );
    affected(player())
);
