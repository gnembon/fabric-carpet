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
        'cylinder <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->['draw_prism', false],
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

set_block(x, y, z, block, replacement)-> (
    existing = block(x, y, z);
    if(block != existing && (!replacement || _block_matches(existing, replacement) ),
        global_positions += bool(set(existing,block))
    );
);

global_positions = 0;

affected(player) -> (
    affected = global_positions;
    print(player,format('gi Filled ' + affected + ' blocks'));

    global_positions = 0;
    affected
);

length_sq(vec) -> reduce(vec, _a + _*_, 0);

fill_flat(pos, offset, dr, rectangle, orientation, block, hollow, replacement)->(
    if(rectangle,
        fill_flat_square(pos, offset, dr, orientation, block, replacement, hollow),
        fill_flat_circle(pos, offset, dr, orientation, block, replacement, hollow)
    )
);

fill_flat_square(pos, offset, radius, orientation, block, replacement, hollow)->(
    [cx,cy,cz]=pos;
    if(orientation=='x',
        for(range(-radius,radius+1),
            r=_;
            if(hollow,
                set_block(cx+offset,cy+radius,cz+r,block,replacement);
                set_block(cx+offset,cy-radius,cz+r,block,replacement);
                set_block(cx+offset,cy+r,cz+radius,block,replacement);
                set_block(cx+offset,cy+r,cz-radius,block,replacement),
                for(range(-radius,radius+1),set_block(cx+offset,cy+_,cz+r,block,replacement))
            )
        ),
        orientation=='y',
        for(range(-radius,radius+1),
            r=_;
            if(hollow,
                set_block(cx+radius,cy+offset,cz+r,block,replacement);
                set_block(cx-radius,cy+offset,cz+r,block,replacement);
                set_block(cx+r,cy+offset,cz+radius,block,replacement);
                set_block(cx+r,cy+offset,cz-radius,block,replacement),
                for(range(-radius,radius+1),set_block(cx+_,cy+offset,cz+r,block,replacement))
            )
        ),
        orientation=='z',
        for(range(-radius,radius+1),
            r=_;
            if(hollow,
                set_block(cx+r,cy+radius,cz+offset,block,replacement);
                set_block(cx+r,cy-radius,cz+offset,block,replacement);
                set_block(cx+radius,cy+r,cz+offset,block,replacement);
                set_block(cx-radius,cy+r,cz+offset,block,replacement),
                for(range(-radius,radius+1),set_block(cx+r,cy+_,cz+offset,block,replacement))
            )
        )
    )
);

fill_flat_circle(pos, offset, radius, orientation, block, replacement, hollow)->(
    [cx,cy,cz]=pos;
    if(orientation=='x',
        for(range(-90, 90, 45/radius),
            cpitch = cos(_)*radius;
            spitch = sin(_)*radius;

            if(hollow,
                set_block(cx+offset,cy+cpitch,cz+spitch,block,replacement);
                set_block(cx+offset,cy-cpitch,cz+spitch,block,replacement),
                for(range(-cpitch,cpitch),
                    set_block(cx+offset,cy+_,cz+cpitch,block,replacement);
                )
            )
        ),
        orientation=='y',
        for(range(-90, 90, round(45/radius)),
            cpitch = round(cos(_)*radius);
            spitch = round(sin(_)*radius);

            if(hollow,
                set_block(cx+cpitch-1,cy+offset,cz+spitch,block,replacement);
                set_block(cx-cpitch,cy+offset,cz+spitch,block,replacement),
                for(range(-cpitch,cpitch),
                    set_block(cx+_,cy+offset,cz+spitch,block,replacement);
                )
            )
        ),
        orientation=='z',
        for(range(-90, 90, 45/radius),
            cpitch = cos(_)*radius;
            spitch = sin(_)*radius;

            if(hollow,
                set_block(cx+spitch,cy+cpitch,cz+offset,block,replacement);
                set_block(cx+spitch,cy-cpitch,cz+offset,block,replacement),
                for(range(-cpitch,cpitch),
                    set_block(cx+cpitch,cy+_,cz+offset,block,replacement);
                )
            )
        )
    )
);

//Drawing commands

draw_sphere(centre, radius, block, replacement, hollow)->(
    if(global_debug, start_time=unix_time());
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

    affected(player());
    if(global_debug,
        end_time=unix_time();
        print(player(),format('gi Time taken: '+(end_time-start_time)+'ms'))
    )
);

draw_diamond(pos, radius, block, replacement)->(
    if(global_debug, start_time=unix_time());

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

    affected(player());
    if(global_debug,
        end_time=unix_time();
        print(player(),format('gi Time taken: '+(end_time-start_time)+'ms'))
    )
);

draw_pyramid(pos, radius, height, pointing, orientation, block, fill_type, replacement, is_square)->(
    if(global_debug, start_time=unix_time());

    hollow = fill_type=='hollow';
    pointup = pointing=='up';
    for(range(height),
        r = if(pointup, radius - radius * _ / height -1, radius * _ / height);
        fill_flat(pos, _, r, is_square, orientation, block, if((pointup&&_==0)||(!pointup && _==height-1),false,hollow),replacement)//Always close bottom off
    );

    affected(player());
    if(global_debug,
        end_time=unix_time();
        print(player(),format('gi Time taken: '+(end_time-start_time)+'ms'))
    )
);

draw_prism(pos, rad, height, orientation, block, fill_type, replacement, is_square)->(
    if(global_debug, start_time=unix_time());

    hollow = fill_type =='hollow';
    radius = rad+0.5;

    for(range(height),
        fill_flat(pos, _, radius, is_square, orientation, block, if(_==0 || _==height-1,false,hollow), replacement)//Always close ends off
    );

    affected(player());
    if(global_debug,
        end_time=unix_time();
        print(player(),format('gi Time taken: '+(end_time-start_time)+'ms'))
    )
);
