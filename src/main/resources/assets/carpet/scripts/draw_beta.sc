import('shapes','draw_sphere', 'draw_diamond', 'draw_filled_diamond', 'draw_pyramid', 'draw_prism'); //importing all the shape funcs from shapes.scl

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
        'pyramid <center> <radius> <height> <pointing> <orientation> <block>'->_(c,r,h,p,o,b)->draw('draw_pyramid',[c,r,h,p,o,'solid', true],b,null),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> hollow'->_(c,r,h,p,o,b)->draw('draw_pyramid',[c,r,h,p,o,'hollow', true],b,null),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid', [c,r,h,p,o,'solid',true],b,rp),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> hollow replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid', [c,r,h,p,o,'hollow',true],b,rp),
        'cone <center> <radius> <height> <pointing> <orientation> <block>'->_(c,r,h,p,o,b)->draw('draw_pyramid',[c,r,h,p,o,'solid',false],b,null),
        'cone <center> <radius> <height> <pointing> <orientation> <block> hollow'->_(c,r,h,p,o,b)->draw('draw_pyramid',[c,r,h,p,o,'hollow',false],b,null),
        'cone <center> <radius> <height> <pointing> <orientation> <block> replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid', [c,r,h,p,o,'solid',false],b,rp),
        'cone <center> <radius> <height> <pointing> <orientation> <block> hollow replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid', [c,r,h,p,o,'hollow',false],b,rp),
        'cuboid <center> <radius> <height> <orientation> <block>'->_(c,r,h,o,b)->draw('draw_prism',[c,r,h,o,'solid', true],b,null),
        'cuboid <center> <radius> <height> <orientation> <block> hollow'->_(c,r,h,o,b)->draw('draw_prism',[c,r,h,o,'hollow',true],b,null),
        'cuboid <center> <radius> <height> <orientation> <block> replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism', [c,r,h,o,'solid',true],b,rp),
        'cuboid <center> <radius> <height> <orientation> <block> hollow replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism', [c,r,h,o,'hollow',true],b,rp),
        'cylinder <center> <radius> <height> <orientation> <block>'->_(c,r,h,o,b)->draw('draw_prism',[c,r,h,o,'solid', false],b,null),
        'cylinder <center> <radius> <height> <orientation> <block> hollow'->_(c,r,h,o,b)->draw('draw_prism',[c,r,h,o,'hollow', false],b,null),
        'cylinder <center> <radius> <height> <orientation> <block> replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism', [c,r,h,o,'solid',false],b,rp),
        'cylinder <center> <radius> <height> <orientation> <block> hollow replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism', [c,r,h,o,'hollow',false],b,rp),
    },
    'arguments'->{
        'center'->{'type'->'pos', 'loaded'->'true'},
        'radius'->{'type'->'int', 'min'->0},
        'replacement'->{'type'->'blockpredicate'},
        'height'->{'type'->'int', 'min'->0},
        'orientation'->{'type'->'term', 'suggest'->['x','y','z']},
        'pointing'->{'type'->'term','suggest'->['up','down']},
    }
};

_block_matches(existing, block_predicate) ->
(
    [name, block_tag, properties, nbt] = block_predicate;

    (name == null || name == existing) &&
    (block_tag == null || block_tags(existing, block_tag)) &&
    all(properties, block_state(existing, _) == properties:_) &&
    (!tag || tag_matches(block_data(existing), tag))
);

//This setter function basically just sets the blocks using the shapes.scl library
draw(what, args, block, replacement)->(
    positions = call(what,args); //Getting the blocks we need to set

    affected = 0;

    for(positions,
        existing = block(_);
        if(block != existing && (!replacement || _block_matches(existing, replacement)),
            affected += bool(set(existing,block)==null) //setting equal to null cos air returns false
        )
    );
    print(format('gi Filled ' + affected + ' blocks'));
);
