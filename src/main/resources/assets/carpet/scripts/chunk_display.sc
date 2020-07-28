global_update_interval = 100;
global_chunk_colors = m(
    // loaded ticking
	l(3,                   l(0xAAdd0000,  0x66990000)), // lime, green
	l(2,                   l(0xFF000000,  0x88000000)), // red
	l(1,                   l(0xFFcc0000,  0xbbaa0000)), // yellowish
	// 'unloaded' (in memory, so rather 'inaccessible')
	l(0,                   l(0xcccccc00,  0x11111100)), // gray / hi constast
	// stable full generation (for '0's, all 1,2,3 are always 'full')
	l('full',              l(0xbbbbbb00,  0x88888800)), // gray
	// unstable final bits
	l('heightmaps',        l(0x33333300,  0x22222200)), // checker
	l('spawn',             l(0x00333300,  0x00222200)), // checker
	l('light_gen',         l(0x55550000,  0x44440000)), // checker
	// stable features
	l('features',          l(0x88bb2200,    0x66882200)), // green muddled
	//stable terrain
	l('liquid_carvers',    l(0x65432100,     0x54321000)), // browns
	//unstable terrain - not generated yet
	l('carvers',            l(0x33003300,  0x22002200)), // checker
	l('surface',            l(0x33330000,  0x22220000)), // checker
	l('noise',              l(0x33000000,  0x22000000)), // checker
	l('biomes',             l(0x00330000,  0x00220000)), // checker
	l('structure_references', l(0x00003300,  0x00002200)), // checker
	// stable
	l('structure_starts',  l(0x66666600,  0x22222200)), // darkgrey
	// not stated yet
	l('empty',             l(0xFF888800,  0xDD444400)), // pink
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
	l('unknown',           l(0xFF55FF00,  0xff99ff00)) // pink purple
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
			global_status_cache:source_pos = l(null, 0);
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
	yval = setup:'plot_center':1+16;
	base_update = global_update_interval;
	random_component = ceil(0.4*base_update);
	duration_max = ceil(1.5*base_update);
	in_dimension( setup:'plot_dimension',
		l(sx, sy, sz) = setup:'source_center';
		l(px, py, pz) = setup:'plot_center';
		source_center = setup:'source_center';
		radius = setup:'radius';
		source_dimension = setup:'source_dimension';

        shapes = l();
        now = tick_time();

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


				if (loaded_status > 0,
					tickets = in_dimension( source_dimension, chunk_tickets(source_pos));
					for(tickets,
						l(type, level) = _;
						if (show_activity || type != 'unknown',
							status = type;
						);
					);
				);
				l(cached_status, expiry) = global_status_cache:source_pos;
				changed = (status != cached_status);
				if ( changed || (expiry < now),
                    global_status_cache:source_pos = l(status, now+base_update+floor(rand(random_component)));
					bpos = l(dx/2, yval, dz/2);
					bcol = global_chunk_colors:status:((dx+dz)%2);
					shapes += l('box', duration_max, 'from', bpos, 'to', bpos + l(0.5,0,0.5),
					    'color', 0xffffff00, 'fill', bcol+128, 'follow', p, 'snap', 'xz');
					if (changed,
					    pbcol = global_chunk_colors:cached_status:((dx+dz)%2);
					    shapes += l('box', 0, 'from', bpos, 'to', bpos + l(0.5,0,0.5),
                            'color', 0xffffff00, 'fill', pbcol+128, 'follow', p, 'snap', 'xz');
					);
				);
			);
		);
		draw_shape(shapes);
	);
	schedule(1, '__chunk_visualizer_tick', p)
)