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
    )),
    'portals' -> {
        'active' -> false,
        'handler' -> '__portal_renderer',
        'tasks' -> {}
    }
);

global_shapes = {'sphere' -> [], 'box' -> []};

__config() ->
{
    'commands' -> {
        'structure <structure>' -> _(s) -> __toggle(s, 'structures'),
        'slime_chunks' -> ['__toggle', 'slime_chunks', 'chunks'],
        'portal coordinates' -> ['__toggle', 'coords', 'portals'],
        'portal links' -> ['__toggle', 'links', 'portals'],
        '<shape> <radius> following <entities>' -> ['display_shape', [null,0xffffffff], true],
        '<shape> <radius> at <entities>' -> ['display_shape', [null,0xffffffff], false],
        '<shape> <radius> following <entities> <color>' -> ['display_shape', true],
        '<shape> <radius> at <entities> <color>' -> ['display_shape', false],
        '<shape> clear' -> 'clear_shape',
        'clear' -> 'clear',
    },
    'arguments' -> {
        'structure' -> {'type' -> 'term', 'suggest' -> plop():'configured_structures' },
        'radius' -> {'type' -> 'int', 'min' -> 0, 'max' -> 1024, 'suggest' -> [128, 24, 32]},
        'shape' -> {'type' -> 'term', 'options' -> keys(global_shapes) },
        'color' -> {'type' -> 'teamcolor'}
    }
};


display_shape(shape, r, entities, color, following) ->
(
    thicc = max(1, floor(6-length(entities)/2));
    fillc = 250 - 100/(length(entities)+1);
    for (entities,
        if (shape == 'sphere',
            shape_config = {
                'center' -> if(following, [0,0,0], pos(_)),
                'radius' -> r
            }
        , shape == 'box',
            shape_config = {
                'from' -> if(following, [-r,-r,-r], pos(_)-r),
                'to' -> if(following, [r,r,r], pos(_)+r)
            }
        );
        if (shape_config,
            if (following, shape_config:'follow' = _);
            shape_config:'fill' = color:1 - fillc;
            shape_config:'color' = color:1;
            shape_config:'line' = thicc;
            draw_shape(shape, 72000, shape_config);
            global_shapes:shape += shape_config;
        )
    )
);


clear_shape(shape) ->
(
    for(global_shapes:shape, draw_shape(shape, 0, _));
    global_shapes:shape = [];
);

clear() ->
(
    for(global_renderers, global_renderers:_:'tasks' = m() );
    for(global_shapes, clear_shape(_));
);

__toggle(feature, renderer) ->
(
    p = player();
    config = global_renderers:renderer;
    if( has(config:'tasks':feature),
        delete(config:'tasks':feature);
        print(p, format('gi disabled '+feature+' overlays'));
    ,
        config:'tasks' += feature;
        if (!config:'active', call(config:'handler', p~'name'));
        print(p, format('gi enabled '+feature+' overlays'));
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
);

__portal_renderer(player_name) ->
(
   l(p, config) = __should_run('portals', player_name);
   if (!p, return());
   dim = p~'dimension';
   shapes = [];
   shape_duration = 6;
   ppos = pos(p);
   py = ppos:1;
   if (dim == 'overworld',
      //
      nether_pos = [ppos:0/8, py, ppos:2/8];
      if (has(config:'tasks':'coords'),
         ow_x = 8*floor(nether_pos:0);
         ow_z = 8*floor(nether_pos:2);
         for (range(-5,6), dx = _;
            for (range(-5,6), dz = _;
               x = ow_x + 8*dx;
               z = ow_z + 8*dz;
               nx = x / 8;
               nz = z / 8;
               shapes += ['line', shape_duration, 'from', [x,0,z], 'to', [x,255,z], 'line', 2];
               shapes += ['box', shape_duration, 'from', [x,0,z], 'to', [x+8,255,z+8], 'color', 0x00000000, 'fill', 0xffffff05];
               shapes += ['label', shape_duration, 'pos', [x+4, 1,z+4], 'text', [nx, floor(py), nz], 'follow', p, 'snap', 'y'];
            )
         );
      );
      if (has(config:'tasks':'links'),
         in_dimension('the_nether',
            portals = map(poi(nether_pos, 16, 'nether_portal', 'any', true), [x,y,z]=_:2; [[x,y,z], [8*x,y,8*z]]);
            if (portals,
               sorted_portals = __sort_portals(nether_pos, portals);
               offset = p~'look' + [0,p~'eye_height',0];
               for(sorted_portals,
                  [pos, display] = _;
                  color = if(_i, 0x00999999, 0x990099ff);
                  to_rel = display-ppos;
                  shapes += ['line', shape_duration, 'line', 5, 'from', [0,0.5,0], 'to', to_rel, 'color', color,'follow', p];

                  to_dist = _euclidean([0,0,0],to_rel);
                  if (to_dist > 3,
                     direction = to_rel / to_dist;
                     shapes += ['label', shape_duration, 'pos', [0,0.5,0]+(2+1*_i)*direction, 'text', pos, 'color', color, 'follow', p];
                  )

               )
            )
         )
      );
   , dim == 'the_nether',
      look = query(p, 'trace', 10, 'blocks', 'exact');
      if (look,
         ly = look:1;
         ow_pos = [8*look:0, look:1, 8*look:2];
         nx = floor(8*look:0)/8;
         nz = floor(8*look:2)/8;
         shapes += ['box', shape_duration, 'from', [nx, ly-1, nz], 'to', [nx+0.125, ly+1, nz+0.125]];
         if (has(config:'tasks':'coords'),
            shapes += ['label', shape_duration, 'pos', look-ppos+[0,0.25,0], 'follow', p, 'text', map(ow_pos,floor(_))];
         );

         if (has(config:'tasks':'links'),
            in_dimension('overworld',
               portals = map(poi(ow_pos, 128, 'nether_portal', 'any', true), [x,y,z]=_:2; [[x,y,z], [x/8,y,z/8]]);
               if (portals,
                  sorted_portals = __sort_portals(ow_pos, portals);
                  for(sorted_portals,
                     [pos, display] = _;
                     color = if(_i, 0x00999999, 0x990099ff);
                     to_rel = display-ppos;
                     shapes += ['line', shape_duration, 'line', 5, 'from', look-ppos+[0,1.0,0], 'to', to_rel, 'color', color, 'follow', p];

                     to_dist = _euclidean([0,0,0],to_rel);
                     if (to_dist > 3,
                        direction = to_rel / to_dist;
                        shapes += ['label', shape_duration, 'pos', look-ppos+[0,1.1,0]+(0.2+0.5*_i)*direction, 'text', pos, 'color', color, 'follow', p]
                     );

                  )
               )
            )
         )
      )
   );
   if (shapes, in_dimension(p, draw_shape(shapes)));
   schedule(5, '__portal_renderer', player_name);
);

__sort_portals(ref_pos, portals) ->
(
   point_map = {};
   for (portals, point_map:(_:0) = _:1);
   sorted_portals = sort_key(keys(point_map), _euclidean_sq(ref_pos, _)+0.0001*(_:1));
   output_locations = [];
   seen_portals = {};
   for (sorted_portals,
      [x,y,z] = _;
      if ( !has(seen_portals, [x,y+1,z]) && !has(seen_portals, [x,y-1,z]),
         output_locations += _;
      );
      seen_portals += _;
   );
   base_portals = [];
   seen_bases = {};
   for (output_locations,
      [x,y,z] = _;
      while(has(seen_portals, [x,y,z]), 256, y += -1);
      y += 1;
      seen_bases += [x,y,z];
      if (!has(seen_bases, [x-1,y,z]) && !has(seen_bases, [x+1,y,z])
       && !has(seen_bases, [x,y,z-1]) && !has(seen_bases, [x,y,z+1]),
         base_portals += [x,y,z];
      )
   );
   map(base_portals, [_, point_map:_]);
);