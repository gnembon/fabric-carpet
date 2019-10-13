__command() ->
(
   print('camera_path module.');
   print('-------------------');
   print(' "/camera_path start" - set the starting point');
   print('');
   print(' "/camera_path add <N>" - add another point <N> frames later');
   print('');
   print(' "/camera_path select_interpolation < linear | gauss_? >"');
   print('    Select interpolation between points:');
   print('    - linear: straight paths between points.');
   print('    - gauss_auto: automatic smooth transitions.');
   print('    - gauss_<number>: custom fixed variance ');
   print('            (in points) for special effects.');
   print('');
   print(' "/camera_path repeat <N> <last_delay>" - ');
   print('    Repeat existing points configuration n-times');
   print('      using <last_delay> points to link path ends');
   print('');
   print(' "/camera_path speed <factor> - ');
   print('    Change number of frames between points');
   print('      from 25 -> 4 times faster (less points),');
   print('      to 400 -> 4 times slower (more points)');
   print('');
   print(' "/camera_path play <fps>: run a path with a player');
   print('    <fps> needs to be multiples of 20, (20 tps)');
   print('');
   print(' "/camera_path show": shows current path for a moment');
   print('');
   print(' "/script in camera_path invoke _show_path_tick <particle> <ppt>":');
   print('    place this in a repeating commandblock');
   print('      to display path continuously');
   print('      with set particle and number of particles per tick');
   print('      example: \'dust 0.9 0.1 0.1 1\' 100 ');
   ''
);

start() ->
(
   p = player();
   global_points = l(l(p ~ 'location', 0,'sharp'));
   undef('global_path_precalculated');
   str('Started path at %.1f %.1f %.1f', p~'x', p~'y', p~'z')
);
add(delay) ->
(
   p = player();
   mode = 'sharp';
   //mode is currently unused, run_path does always sharp, gauss interpolator is always smooth
   // but this option could be used could be used at some point by more robust interpolators
   __add_path_segment(p ~ 'location', delay, mode);
   str('Added point %d: %.1f %.1f %.1f', length(global_points), p~'x', p~'y', p~'z')
);
__add_path_segment(vector, duration, mode) ->
(
   undef('global_path_precalculated');
   if ( (l('sharp','smooth') ~ mode) == null, exit('use smooth or sharp point'));
   if(!global_points, exit('Cannot add point to path that didn\'t started yet!'));
   l(v, end_time, m) = global_points:(-1);
   vector:(-2) = __adjusted_rot(v:(-2) , vector:(-2) );
   global_points += l(vector, end_time+duration, mode)
);
// adjusts current rotation so we don\'t spin around like crazy
__adjusted_rot(previous_rot, current_rot) ->
(
   while( abs(previous_rot-current_rot) > 180, 1000,
       current_rot += if(previous_rot < current_rot, -360, 360)
   );
   current_rot
);


repeat(times, last_section_duration) ->
(
   undef('global_path_precalculated');
   positions = map(global_points, _:0);
   modes = map(global_points, _:(-1));
   durations = map(global_points, global_points:(_i+1):1 - _:1 );
   durations:(-1) = last_section_duration;
   loop(times,
       loop( length(positions),
           __add_path_segment(positions:_, durations:_, modes:_)
       )
   );
   str('Add %d points %d times', length(positions), times)
);
speed(percentage) ->
(
   undef('global_path_precalculated');
   if (percentage < 25 || percentage > 400,
       exit('path speed can only be speed, or slowed down 4 times. Recall command for larger changes')
   );
   ratio = percentage/100;
   previous_path_length = global_points:(-1):1;
   for(global_points, _:1 = _:1*ratio );
   undef('global_path_precalculated');
   str('path %s from %d to %d ticks',
       if(ratio<1,'shortened','extended'),
       previous_path_length,
       global_points:(-1):1
   )
);
select_interpolation(method) ->
(
   undef('global_path_precalculated');
   __prepare_path_if_needed() -> __prepare_path_if_needed_generic();
   // each supported method needs to specify its __find_position_for_point to trace the path
   // accepting segment number, and position in the segment
   // or optionally __prepare_path_if_needed, if path is inefficient to compute point by point
   if (
       method == 'linear',
       (
           __find_position_for_point(s, p) -> __find_position_for_linear(s, p)
       ),
       method ~ '^gauss_',
       (
           type = method - 'gauss_';
           global_interpol_option = if(type=='auto',0,number(type));
           __find_position_for_point(s, p) -> __find_position_for_gauss(s, p)
       ),

       exit('Choose one of the following methods: linear, gauss:auto, gauss:<deviation>')
   );
   'Ok'
);
select_interpolation('gauss_auto');
__assert_valid_for_motion() ->
(
   if(!global_points, exit('Path not defined yet'));
   if(length(global_points)<2, exit('Path not complete - add more points'));
   null
);
__get_path_at(segment, start, index) ->
(
   v = global_path_precalculated:(start+index);
   if(v == null,
       v = __find_position_for_point(segment, index);
       global_path_precalculated:(start+index) =  v
    );
    v
);
__invalidate_points_cache() -> global_path_precalculated = map(range(global_points:(-1):1), null);
show() ->
(
   loop(100,
       _show_path_tick('dust 0.9 0.1 0.1 1', 100);
       game_tick(50)
   );
   'Done!'
);
play(fps) ->
(
   p = player();
   __assert_valid_for_motion();
   __prepare_path_if_needed();
   if ((fps % 20 != 0) || fps < 20, exit('FPS needs to be multiples of 20') );
	tpt = round(fps / 20);
   mspt = 50 / tpt;
   start_time = time();
   point = 0;
   loop( length(global_points)-1,
       segment = _;
       start = global_points:segment:1;
       end = global_points:(segment+1):1;
       loop(end-start,
           v = __get_path_at(segment, start, _);
           modify(p, 'location', v);
           point += 1;
           if ((point % tpt == 0), game_tick());
           end_time = time();
           took = end_time - start_time;
           if (took < mspt, sleep(mspt-took));
           start_time = time()
       )
   );
   game_tick(1000);
   'Done!'
);
_show_path_tick(particle_type, total) ->
(
   __assert_valid_for_motion();
   __prepare_path_if_needed();
   loop(total,
       segment = floor(rand(length(global_points)-1));
       start = global_points:segment:1;
       end = global_points:(segment+1):1;
       index = floor(rand(end-start));
       l(x, y, z) = slice(__get_path_at(segment, start, index), 0, 3);
       particle(particle_type, x, y, z, 1, 0, 0)
   );
   null
);
__prepare_path_if_needed_generic() ->
(
   if(!global_path_precalculated, __invalidate_points_cache())
);
__find_position_for_linear(segment, point) ->
(
   l(va, start, mode_a) = global_points:segment;
   l(vb, end, mode_b)   = global_points:(segment+1);
   section = end-start;
   dt = point/section;
   dt*vb+(1-dt)*va
);
//(1/sqrt(2*pi*d*d))*euler^(-((x-miu)^2)/(2*d*d))
// but we will be normalizing anyways, so who cares
__norm_prob(x, miu, d) -> euler^(-((x-miu)^2)/(2*d*d));
__find_position_for_gauss(from_index, point) ->
(
   dev = global_interpol_option;
   components = l();
   path_point = global_points:from_index:1;

   try(
       for(range(from_index+1, length(global_points)),
           l(v,ptime,mode) = global_points:_;
           dev = if (global_interpol_option > 0, global_interpol_option,
               devs = l();
               if (_+1 < length(global_points), devs += global_points:(_+1):1-ptime);
               if (_-1 >= 0, devs += ptime-global_points:(_-1):1);
               0.6*reduce(devs, _a+_, 0)/length(devs)
           );
           impact = __norm_prob(path_point+point, ptime, dev);
           if(rtotal && impact < 0.000001*rtotal, throw());
           components += l(v, impact);
           rtotal += impact
       )
   );
   try(
       for(range(from_index, -1, -1),
           l(v,ptime,mode) = global_points:_;
           dev = if (global_interpol_option > 0, global_interpol_option,
               devs = l();
               if (_+1 < length(global_points), devs += global_points:(_+1):1-ptime);
               if (_-1 >= 0, devs += ptime-global_points:(_-1):1);
               0.6*reduce(devs, _a+_, 0)/length(devs)
           );
           impact = __norm_prob(path_point+point, ptime, dev);
           if(ltotal && impact < 0.000001*ltotal, throw());
           components += l(v, impact);
           ltotal += impact
       )
   );
   total = rtotal+ltotal;
   reduce(components, _a+_:0*(_:1/total), l(0,0,0,0,0))
)