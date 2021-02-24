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
