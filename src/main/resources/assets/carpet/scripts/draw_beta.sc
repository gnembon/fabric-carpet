import('shapes','draw_sphere', 'draw_diamond', 'draw_filled_diamond', 'draw_pyramid', 'draw_prism'); //importing all the shape funcs from the other app

__config() -> {
    'commands'->{
        'sphere <center> <radius> <block>'->_(c,r,b)->draw('draw_sphere',[c,r, true],b,null),
        'sphere <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_sphere',[c,r,true],b,rp),
        'ball <center> <radius> <block>'->_(c,r,b)->draw('draw_sphere',[c,r,false],b,null),
        'ball <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_sphere',[c,r,false],b,rp),
        'diamond <center> <radius> <block> hollow' -> _(c,r,b)->draw('draw_diamond',[c,r],b,null),
        'diamond <center> <radius> <block> hollow replace <replacement>'->_(c,r,b,rp)->draw('draw_diamond',[c, r], b, rp),
        'diamond <center> <radius> <block>' -> _(c,r,b)->draw('draw_filled_diamond',[c,r],b,null),
        'diamond <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_filled_diamond',[c, r], b, rp),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->_(c,r,h,p,o,b,ho)->draw('draw_pyramid',[c,r,h,p,o,ho, true],b,null),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,p,o,b,ho,rp)->draw('draw_pyramid', [c,r,h,p,o,ho,true],b,rp),
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow>'->_(c,r,h,p,o,b,ho)->draw('draw_pyramid',[c,r,h,p,o,ho, false],b,null),
        'cone <center> <radius> <height> <pointing> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,p,o,b,ho,rp)->draw('draw_pyramid', [c,r,h,p,o,ho,false],b,rp),
        'cuboid <center> <radius> <height> <orientation> <block> <hollow>'->_(c,r,h,o,b,ho)->draw('draw_prism',[c,r,h,p,o,ho, true],b,null),
        'cuboid <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,o,b,ho,rp)->draw('draw_prism', [c,r,h,o,ho,true],b,rp),
        'cylinder <center> <radius> <height> <orientation> <block> <hollow>'->_(c,r,h,o,b,ho)->draw('draw_prism',[c,r,h,p,o,ho, false],b,null),
        'cylinder <center> <radius> <height> <orientation> <block> <hollow> replace <replacement>'->_(c,r,h,o,b,ho,rp)->draw('draw_prism', [c,r,h,o,ho,false],b,rp),
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

_block_matches(existing, block_predicate) ->
(
    [name, block_tag, properties, nbt] = block_predicate;

    (name == null || name == existing) &&
    (block_tag == null || block_tags(existing, block_tag)) &&
    all(properties, block_state(existing, _) == properties:_) &&
    (!tag || tag_matches(block_data(existing), tag))
);

draw(what, args, block, replacement)->(//custom setter cos it's easier
    positions = call(what,args); //returning blocks to be set

    affected = 0;

    for(positions,
        existing = block(_);
        if(block != existing && (!replacement || _block_matches(existing, replacement)),
            affected += bool(set(existing,block))
        )
    );
    print(format('gi Filled ' + affected + ' blocks'));
);
