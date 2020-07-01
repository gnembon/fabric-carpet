import('math', '_euclidean_sq', '_euclidean');

global_renderers = m(
    l('structures', m(
        l('active', false),
        l('handler', '__structure_renderer'),
        l('tasks', m()),
        l('range', 3),
        l('max_pieces', 6)
    )),
    l('chunks', m(
        l('active', false),
        l('handler', '__chunk_renderer'),
        l('tasks', m()),
        l('range', 6)
    )),
    l('shapes', m(
        l('active', false),
        l('handler', '__shape_renderer'),
        l('tasks', m()),
        l('range', 8)
    ))
);

__command() -> '';

monument() -> __toggle('monument', 'structures');
fortress() -> __toggle('fortress', 'structures');
mansion() -> __toggle('mansion', 'structures');
jungle_temple() -> __toggle('jungle_temple', 'structures');
desert_temple() -> __toggle('desert_temple', 'structures');
end_city() -> __toggle('end_city', 'structures');
igloo() -> __toggle('igloo', 'structures');
shipwreck() -> __toggle('shipwreck', 'structures');
witch_hut() -> __toggle('witch_hut', 'structures');
stronghold() -> __toggle('stronghold', 'structures');
ocean_ruin() -> __toggle('ocean_ruin', 'structures');
treasure() -> __toggle('treasure', 'structures');
pillager_outpost() -> __toggle('pillager_outpost', 'structures');
mineshaft() -> __toggle('mineshaft', 'structures');
village() -> __toggle('village', 'structures');
slime_chunks() -> __toggle('slime_chunks', 'chunks');

clear() -> for(global_renderers, global_renderers:_:'tasks' = m() );

__toggle(feature, renderer) ->
(
    p = player();
    config = global_renderers:renderer;
    if( has(config:'tasks':feature),
        delete(config:'tasks':feature);
        'disabled '+feature+' overlays';
    ,
        config:'tasks' += feature;
        if (!config:'active', call(config:'handler', p~'name'));
        'enabled '+feature+' overlays';
    )
);

__should_run(renderer, player_name) ->
(
    config = global_renderers:renderer;
    if (length(config:'tasks')==0, config:'active' = false; return(l(null, null)));
    p = player(player_name);
    if (!p, config:'active' = false; clear(); return(l(null, null)));
    config:'active' = true;
    l(p, config);
);

__structure_renderer(player_name) ->
(
    l(p, config) = __should_run('structures', player_name);
    if (!p, return());
	in_dimension(p,
	    ppos = pos(p);
	    starts = m();
	    r = config:'range';
		for(range(-r,r), cx =_;
			for (range(-r,r), cz = _;
				ref_pos = ppos + l(16*cx,0,16*cz);
				for(filter(structure_references(ref_pos), has(config:'tasks':_)),
				    name = _;
				    for(structure_references(ref_pos, name),
				        starts += l(_, name);
				    )
				)
			)
		);
		for (starts, l(position, name) = _;
		    structure_data = structures(position):name;
		    if (!structure_data, continue()); // messed up references - shouldn't happen
		    structure_pieces = structures(position, name):'pieces';
		    if (!structure_pieces, continue()); // messed up references - shouldn't happen
		    l(from, to) = structure_data;
		    total_size = _euclidean(from, to);
		    density = max(10, total_size/10);
		    draw_shape('box', 15, 'from', from, 'to', to+1, 'color', 0x00FFFFFF, 'line', 3, 'fill', 0x00FFFF12);
		    structure_pieces = slice(sort_key(structure_pieces, _euclidean_sq((_:2+_:3)/2,ppos)), 0, config:'max_pieces');
		    for (structure_pieces, l(piece, direction, from, to) = _;
		        r = 255 - floor(128 * _i/config:'max_pieces');
		        g = r;
		        b = 255;
		        a = 255;
		        color = 256*(b+256*(g+256*r)); // leaving out a
		        draw_shape('box', 15, 'from', from, 'to', to+1, 'color', color+a);
		        draw_shape('box', 15, 'from', from, 'to', to+1, 'color', 0xffffff00, 'fill', 0xffffff22);
		    )
		)
	);
	schedule(10, '__structure_renderer', player_name);
);

__chunk_renderer(player_name) ->
(
    l(p, config) = __should_run('chunks', player_name);
    if (!p, return());
    in_dimension( p,
        // get lower corner of the chunk
        ppos = map(pos(p)/16, floor(_))*16;
        ppos:1 = 0;
        rang = config:'range';
        for(range(-rang,rang), cx =_;
        	for (range(-rang,rang), cz = _;
        	    ref_pos = ppos + l(16*cx,0,16*cz);
                if(has(config:'tasks':'slime_chunks') && in_slime_chunk(ref_pos),
                    player_distance = _euclidean(ppos, ref_pos);
                    top_00 = ref_pos + l(0, top('terrain', ref_pos)+10, 0);
                    top_11 = ref_pos + l(16, top('terrain', ref_pos+l(15,0,15))+10, 16);
                    top_10 = ref_pos + l(16, top('terrain', ref_pos+l(15, 0, 0))+10, 0);
                    top_01 = ref_pos + l(0, top('terrain', ref_pos+l(0, 0, 15))+10, 16);
                    r = 30;
                    g = 220;
                    b = 30;
                    a = max(0, 255-player_distance);
                    color = a+256*(b+256*(g+256*r));
                    draw_shape(l(
                    l('line', 15, 'from', top_00, 'to', top_10, 'color', color, 'line', 3),
                    l('line', 15, 'from', top_10, 'to', top_11, 'color', color, 'line', 3),
                    l('line', 15, 'from', top_11, 'to', top_01, 'color', color, 'line', 3),
                    l('line', 15, 'from', top_01, 'to', top_00, 'color', color, 'line', 3),
                    l('line', 15, 'from', top_00, 'to', top_11, 'color', color, 'line', 3),
                    l('line', 15, 'from', top_01, 'to', top_10, 'color', color, 'line', 3)
                    ));
                )
        	)
        )
    );
    schedule(10, '__chunk_renderer', player_name);
)