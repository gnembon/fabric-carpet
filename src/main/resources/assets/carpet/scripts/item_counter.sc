
 'run it every tick to collect items that tick without bells and whistles';
 _counter_tick() -> (
    if(global_counter_area,
        l(x, y, z, dx, dy, dz) = global_counter_area;
        for( entity_area('items',x, y, z, dx, dy, dz),
            var('global_counter_item_'+(_ ~ 'item')) += _ ~ 'count';
            modify(_, 'remove')
        )
    );
    null
 );

 'run it every tick to collect items that tick WITH bells and whistles';
 _counter_tick_visual() -> (
    if(global_counter_area,
        l(x, y, z, dx, dy, dz) = global_counter_area;
        total = for( entity_area('items',x, y, z, dx, dy, dz),
            var('global_counter_item_'+(_ ~ 'item')) += _ ~ 'count';
            modify(_, 'remove')
        );
        if(total, particle_rect('dust 0.8 0.8 0.8 1',x-dx,y-dy,z-dz,x+dx,y+dy,z+dz,4))
    ,
        0
    )
 );

 'run /script invokepoint counter_start ... args to setup a counter';
 counter_start(x, y, z, dx, dy, dz) -> (
    counter_remove();
    global_counter_area = l(x, y, z, dx, dy, dz);
    global_counter_tickstart = tick_time();
    particle_rect('dust 0.1 0.8 0.1 1',x-dx,y-dy,z-dz,x+dx,y+dy,z+dz,0.1);
    str('Counter started around %d %d %d', x, y, z)
 );	

 'run /script invoke counter_reset to restart already running counter';
 counter_reset() -> (
    if(!global_counter_area, exit('Counter hasn\'t been setup yet, call counter_start'));
    for(vars('global_counter_item_'), undef(_) );
    global_counter_tickstart = tick_time();
    l(x, y, z, dx, dy, dz) = global_counter_area;
    particle_rect('dust 0.8 0.1 0.1 1',x-dx,y-dy,z-dz,x+dx,y+dy,z+dz,0.1);
    str('Counter reset around %d %d %d', x, y, z)
 );

 'run /script invoke counter_remove to stop counting items';
 counter_remove() -> ( for(vars('global_counter_'), undef(_) ); 'Counter removed' );

 'to list drop and drop rates => /script invoke counter';
 counter() -> (
    if(!global_counter_area, exit('Counter hasn\'t been setup yet, call counter_start'));
    l(x, y, z, dx, dy, dz) = global_counter_area;
    particle_rect('dust 0.8 0.8 0.1 1',x-dx,y-dy,z-dz,x+dx,y+dy,z+dz,0.1);
    current_counts = map(vars('global_counter_item_'), l(_ - 'global_counter_item_', var(_)));
    duration = (tick_time()-global_counter_tickstart)/(20*60*60);
    if (!current_counts || !duration, exit('No items has been counted yet'));
    for( sort_key(current_counts, -element(_,1)),
        l(item, count) = _;
        total += count;
        print(str('%s: %d total, %d/hr', item, count, count/duration))
    );
    print('------------');
    str('Total: %d, %d/hr', total, total/duration)
 )

