global_chunk_colors = m(
    // loaded ticking
	l(3,                   l(0xBBFF0000,  0x88bb0000)), // lime, green
	l(2,                   l(0xFF000000,  0x88000000)), // red
	l(1,                   l(0xFFFF0000,  0xbbbb0000)), // yellow
	// 'unloaded' (in memory, so rather 'inaccessible')
	l(0,                   l(0xcccccc00,  0x11111100)), // gray / hi constast
	// stable full generation (for '0's, all 1,2,3 are always 'full')
	l('full',              l(0xbbbbbb00,  0x88888800)), // gray
	// unstable final bits
	l('heightmaps',        l(0xFFFFFF00,  0x00000000)), // check
	l('spawn',             l(0xFFFFFF00,  0x00000000)), // check
	l('light_gen',         l(0xFFFFFF00,  0x00000000)), // check
	// stable features
	l('features',          l(0x88bb2200,    0x66882200)), // green muddled
	//stable terrain
	l('liquid_carvers',    l(0x65432100,     0x54321000)), // browns
	//unstable terrain - not generated yet
	l('carvers',            l(0xFFFFFF00,  0x00000000)), // check
	l('surface',            l(0xFFFFFF00,  0x00000000)), // check
	l('noise',              l(0xFFFFFF00,  0x00000000)), // check
	l('biomes',             l(0xFFFFFF00,  0x00000000)), // check
	l('structure_references', l(0xFFFFFF00,  0x00000000)), // check
	// stable
	l('structure_starts',  l(0x66666600,  0x22222200)), // darkgrey
	// not stated yet
	l('empty',             l(0xFF88888800,  0xDD444400)), // pink
	// proper not in memory
	l(null ,               l(0xFFFFFF00,  0xFFFFFF00)), // hwite

	// ticket types
	l('player',            l(0x0000DD00,  0x00008800)), // blue
	l('portal',            l(0xAA00AA00,  0xAA00AA00)), // purple
	l('dragon',            l(0xAA008800,  0xAA008800)), // purple
	l('forced',            l(0xAABBFF00,  0x8899AA00)), // blue
	l('light',             l(0xFFFF0000,  0xBBBB0000)), // yellow
	l('post_teleport',     l(0xAA00AA00,  0xAA00AA00)), // purple
	l('start',             l(0xDDFF0000,  0xDDFF0000)), // lime, green
	// recent chunk requests
	l('unknown',           l(0xFFFFFF00,  0x00000000)) // check
);

// map representing current displayed chunkloading setup
global_current_setup = null;

// which blocks in the inventory represent which dimension
global_block_markers = m(
	l('netherrack', 'the_nether'),
	l('grass_block', 'overworld'),
	l('end_stone', 'the_end')
);

__on_player_uses_item(player, item_tuple, hand) ->
(
	if (hand != 'mainhand', return());
	if (!has(global_block_markers, item_tuple:0), return());
	if (item_tuple:1%16 != 0, return());
	new_setup = true;
	if (global_current_setup,
		new_setup = __remove_previous_setup(player)
	);
	if (!new_setup, return());
	tag = nbt('{}');
	put(tag:'Enchantments','[]');
	put(tag:'Enchantments', '{lvl:1s,id:"minecraft:protection"}', 0);
	inventory_set(player, player~'selected_slot', item_tuple:1, item_tuple:0, tag);
	__setup_tracker(player, item_tuple);
);

//__on_tick() -> null;

__remove_previous_setup(new_player) ->
(
	if (!global_current_setup, return());
	global_running = false;
	p = player();
	if (p &&
			global_block_markers:((item = inventory_get(p, global_current_setup:'inventory_slot')):0)==global_current_setup:'source_dimension' &&
			item:1 == global_current_setup:'radius' &&
			item:2,
		inventory_set(p, global_current_setup:'inventory_slot', item:1, item:0, null); // disenchant
		if( new_player == p && new_player~'selected_slot' == global_current_setup:'inventory_slot',
			disabled = true
		);
	);
	global_current_setup = null;
	!disabled
);

__setup_tracker(player, item) ->
(
	setup = m();
	setup:'player_name' = str(player);
	setup:'inventory_slot' = player ~ 'selected_slot';
	setup:'plot_dimension' = player ~ 'dimension';
	setup:'plot_center' = map(pos(player), floor(_))-l(0,1,0);
	setup:'radius' = item:1;
	setup:'source_dimension' = global_block_markers:(item:0);
	multiplier = 1;
	if (setup:'source_dimension' != setup:'plot_dimension',
		multiplier = if (
			setup:'source_dimension' == 'the_nether', 1/8,
			setup:'source_dimension' == 'the_end', 0,
			8);
	);
	setup:'source_center' = setup:'plot_center' * multiplier;
	print(setup:'source_dimension'+' around '+setup:'source_center'+' with '+setup:'radius'+' radius ('+ (2*setup:'radius'+1)^2 +' chunks)');

	l(sx, sy, sz) = setup:'source_center';
	global_status_cache = m();
	loop( 2*setup:'radius'+1, dx = _ - setup:'radius';
		loop( 2*setup:'radius'+1, dz = _ - setup:'radius';
			source_pos = l(sx+16*dx,sy,sz+16*dz);
			global_status_cache:source_pos = null
		)
	);
	global_current_setup = setup;
	global_running = true;
	schedule(0, '__chunk_visualizer_tick', player);
);

global_status_cache = m();

__chunk_visualizer_tick(p) ->
(
	setup = global_current_setup;
	if(!setup, return());
	if (!global_running, return());
	show_activity = (p~'holds':0 == 'redstone_torch');
	in_dimension( setup:'plot_dimension',
		l(sx, sy, sz) = setup:'source_center';
		l(px, py, pz) = setup:'plot_center';
		source_center = setup:'source_center';
		radius = setup:'radius';
		source_dimension = setup:'source_dimension';

        shapes = l();

		loop( 2*radius+1, dx = _ - radius;
			loop( 2*radius+1, dz = _ - radius;
				changed = false;
				source_pos = l(sx+16*dx,sy,sz+16*dz);
				status = in_dimension( source_dimension,
					loaded_status = loaded_status(source_pos);
					if (loaded_status > 0, loaded_status, generation_status(source_pos))
				);
				// will mix with light ticket
				if (status=='light', status = 'light_gen');

				if ( status != global_status_cache:source_pos,
					global_status_cache:source_pos = status;
					changed = true;
				);
				if (loaded_status > 0,
					tickets = in_dimension( source_dimension, chunk_tickets(source_pos));
					for(tickets,
						l(type, level) = _;
						if (show_activity || type != 'unknown',
							global_status_cache:source_pos = type;
							changed = true;
						);
					);
				);
				//if (changed,
					//to = global_status_cache:source_pos;
					//if(!has(global_chunk_colors:to), print('problem with '+to));
					//set(px+dx,py,pz+dz, global_chunk_colors:to:((dx+dz)%2));
					//set(px+dx,py,pz+dz, global_chunk_colors:(global_status_cache:source_pos):((dx+dz)%2));
					//bpos = l(px+dx/2, py, pz+dz/2);
					bpos = l(dx/2, -10, dz/2);
					bcol = global_chunk_colors:(global_status_cache:source_pos):((dx+dz)%2);
					shapes += l('box', 10, 'from', bpos, 'to', bpos + l(0.5,0,0.5), 'color', bcol, 'fill', bcol+128,
					'follow', p)
					;
				//);
			);
		);
		draw_shape(shapes);
	);
	schedule(5, '__chunk_visualizer_tick', p)
)