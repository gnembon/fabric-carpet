package carpet.script.bundled;

public class CameraPathModule implements ModuleInterface
{
    @Override
    public String getName()
    {
        return "camera";
    }

    @Override
    public String getCode()
    {
        return  " __command() ->\n" +
                " (\n" +
                "   print('Camera path module.');\n" +
                "   print('-------------------');\n" +
                "   print(' \"/camera start\" - set the starting point');\n" +
                "   print('');\n" +
                "   print(' \"/camera add <N>\" - add another point <N> frames later');\n" +
                "   print('');\n" +
                "   print(' \"/camera select_interpolation < linear | gauss_? >\"');\n" +
                "   print('    Select interpolation between points:');\n" +
                "   print('    - linear: straight paths between points.');\n" +
                "   print('    - gauss_auto: automatic smooth transitions.');\n" +
                "   print('    - gauss_<number>: custom fixed variance ');\n" +
                "   print('            (in points) for special effects.');\n" +
                "   print('');\n" +
                "   print(' \"/camera repeat <N> <last_delay>\" - ');\n" +
                "   print('    Repeat existing points configuration n-times');\n" +
                "   print('      using <last_delay> points to link path ends');\n" +
                "   print('');\n" +
                "   print(' \"/camera speed <factor> - ');\n" +
                "   print('    Change number of frames between points');\n" +
                "   print('      from 25 -> 4 times faster (less points),');\n" +
                "   print('      to 400 -> 4 times slower (more points)');\n" +
                "   print('');\n" +
                "   print(' \"/camera play <fps>: run a path with a player');\n" +
                "   print('    <fps> needs to be multiples of 20, (20 tps)');\n" +
                "   print('');\n" +
                "   print(' \"/camera show\": shows current path for a moment');\n" +
                "   print('');\n" +
                "   print(' \"/script in camera invoke _show_path_tick <particle> <ppt>\":');\n" +
                "   print('    place this in a repeating commandblock');\n" +
                "   print('      to display path continuously');\n" +
                "   print('      with set particle and number of particles per tick');\n" +
                "   print('      example: \\'dust 0.9 0.1 0.1 1\\' 100 ');\n" +
                "   ''\n" +
                " );\n" +
                " start() -> \n" +
                "(\n" +
                "   p = player();\n" +
                "   global_points = l(l(l(p~'x',p~'y',p~'z',p~'yaw',p~'pitch'),0,'sharp'));\n" +
                "   undef('global_path_precalculated');\n" +
                "   str('Started path at %.1f %.1f %.1f', p~'x', p~'y', p~'z')\n" +
                ");\n" +
                "\n" +
                " add(delay) -> \n" +
                "( \n" +
                "   p = player(); \n" +
                "   mode = 'sharp';\n" +
                "   'mode is currently unused, run_path does always sharp, gauss interpolator is always smooth';\n" +
                "   ' but this option could be used could be used at some point by more robust interpolators';\n" +
                "   vector = l(p~'x',p~'y',p~'z', p~'yaw', p~'pitch');\n" +
                "   __add_path_segment(vector, delay, mode);\n" +
                "   str('Added point %d: %.1f %.1f %.1f', length(global_points), p~'x', p~'y', p~'z')\n" +
                ");\n" +
                "\n" +
                " __add_path_segment(vector, duration, mode) -> \n" +
                "(\n" +
                "   undef('global_path_precalculated');\n" +
                "   if ( (l('sharp','smooth') ~ mode) == null, exit('use smooth or sharp point'));  \n" +
                "   if(!global_points, exit('Cannot add point to path that didn\\'t started yet!'));\n" +
                "   l(v, end_time, m) = element(global_points, -1);\n" +
                "   put(vector,-2, __adjusted_rot(element(v, -2), element(vector, -2)));\n" +
                "   global_points += l(vector, end_time+duration, mode)\n" +
                ");\n" +
                "\n" +
                "'adjusts current rotation so we don\\'t spin around like crazy';\n" +
                " __adjusted_rot(previous_rot, current_rot) -> \n" +
                "(\n" +
                "   while( abs(previous_rot-current_rot) > 180, 1000,\n" +
                "       current_rot += if(previous_rot < current_rot, -360, 360)\n" +
                "   );\n" +
                "   current_rot\n" +
                ");\n" +
                "\n" +
                "\n" +
                " repeat(times, last_section_duration) -> \n" +
                "(\n" +
                "   undef('global_path_precalculated');\n" +
                "   positions = map(global_points, k = element(_, 0));\n" +
                "   modes = map(global_points, element(_, -1));\n" +
                "   durations = map(global_points, element(element(global_points, _i+1), 1)-element(_, 1));\n" +
                "   put(durations, -1, last_section_duration);\n" +
                "   loop(times,\n" +
                "       loop( length(positions),\n" +
                "           __add_path_segment(element(positions, _), element(durations, _), element(modes, _))\n" +
                "       )\n" +
                "   );\n" +
                "   str('Add %d points %d times', length(positions), times)\n" +
                ");\n" +
                "\n" +
                " speed(percentage) ->\n" +
                "(\n" +
                "   undef('global_path_precalculated');\n" +
                "   if (percentage < 25 || percentage > 400, \n" +
                "       exit('path speed can only be speed, or slowed down 4 times. Recall command for larger changes')\n" +
                "   );\n" +
                "   ratio = percentage/100;\n" +
                "   previous_path_length = element(element(global_points, -1),1);\n" +
                "   for(global_points, put(_, 1, element(_, 1)*ratio ) );\n" +
                "   undef('global_path_precalculated');\n" +
                "   str('path %s from %d to %d ticks',\n" +
                "       if(ratio<1,'shortened','extended'),\n" +
                "       previous_path_length,\n" +
                "       element(element(global_points, -1),1)\n" +
                "   )\n" +
                ");\n" +
                "\n" +
                " select_interpolation(method) ->\n" +
                "(\n" +
                "   undef('global_path_precalculated');\n" +
                "   __prepare_path_if_needed() -> __prepare_path_if_needed_generic();\n" +
                "   'each supported method needs to specify its __find_position_for_point to trace the path';\n" +
                "   'accepting segment number, and position in the segment';\n" +
                "   'or optionally __prepare_path_if_needed, if path is inefficient to compute point by point';\n" +
                "   if (\n" +
                "       method == 'linear',\n" +
                "       (\n" +
                "           __find_position_for_point(s, p) -> __find_position_for_linear(s, p)\n" +
                "       ),\n" +
                "       method ~ '^gauss_',\n" +
                "       (\n" +
                "           type = method - 'gauss_';\n" +
                "           global_interpol_option = if(type=='auto',0,number(type));\n" +
                "           __find_position_for_point(s, p) -> __find_position_for_gauss(s, p)\n" +
                "       ),\n" +
                "       method ~ '^bungee_',\n" +
                "       (\n" +
                "           exit('unsupported / planned');\n" +
                "           type = method - 'bungee_';\n" +
                "           global_interpol_option = if(type=='auto',80,number(type));\n" +
                "           __prepare_path_if_needed() -> __prepare_path_if_needed_bungee()\n" +
                "       ),\n" +
                "       \n" +
                "       exit('Choose one of the following methods: linear, gauss:auto, gauss:<deviation>')\n" +
                "   );\n" +
                "   'Ok'\n" +
                ");\n" +
                "\n" +
                " select_interpolation('linear');\n" +
                "\n" +
                " __assert_valid_for_motion() ->\n" +
                "(\n" +
                "   if(!global_points, exit('Path not defined yet'));\n" +
                "   if(length(global_points)<2, exit('Path not complete - add more points'));\n" +
                "   null    \n" +
                ");\n" +
                "\n" +
                " __get_path_at(segment, start, index) ->\n" +
                "(\n" +
                "   v = element(global_path_precalculated, start+index);\n" +
                "   if(v == null,\n" +
                "       v = __find_position_for_point(segment, index);\n" +
                "       put(global_path_precalculated, start+index, v)\n" +
                "    );\n" +
                "    v\n" +
                ");\n" +
                "\n" +
                " __invalidate_points_cache() -> global_path_precalculated = map(range(element(element(global_points, -1),1)), null);\n" +
                "\n" +
                " show() -> \n" +
                "(\n" +
                "   loop(100,\n" +
                "       _show_path_tick('dust 0.9 0.1 0.1 1', 100);\n" +
                "       game_tick(50)\n" +
                "   );\n" +
                "   'Done!'\n" +
                ");\n" +
                "\n" +
                " __play(fps) -> \n" +
                "(\n" +
                "   p = player();\n" +
                "   __assert_valid_for_motion();\n" +
                "   __prepare_path_if_needed();\n" +
                "   loop( length(global_points)-1,\n" +
                "       segment = _;\n" +
                "       start = element(element(global_points, segment),1);\n" +
                "       end = element(element(global_points, segment+1),1);\n" +
                "       loop(end-start,\n" +
                "           v = __get_path_at(segment, start, _);\n" +
                "           modify(p, 'location', v);\n" +
                "           game_tick(1000/fps)\n" +
                "       )\n" +
                "   );\n" +
                "   game_tick(1000);\n" +
                "   'Done!'\n" +
                ");\n" +
                "\n" +
                " play(fps) -> \n" +
                "(\n" +
                "   p = player();\n" +
                "   __assert_valid_for_motion();\n" +
                "   __prepare_path_if_needed();\n" +
                "   if ((fps % 20 != 0) || fps < 20, exit('FPS needs to be multiples of 20') );\n" +
                "\ttpt = round(fps / 20);\n" +
                "   mspt = 50 / tpt; \n" +
                "   start_time = time();\n" +
                "   point = 0;\n" +
                "   loop( length(global_points)-1,\n" +
                "       segment = _;\n" +
                "       start = element(element(global_points, segment),1);\n" +
                "       end = element(element(global_points, segment+1),1);\n" +
                "       loop(end-start,\n" +
                "           v = __get_path_at(segment, start, _);\n" +
                "           modify(p, 'location', v);\n" +
                "           point += 1;\n" +
                "           if ((point % tpt == 0), game_tick());\n" +
                "           end_time = time();\n" +
                "           took = end_time - start_time;\n" +
                "           if (took < mspt, sleep(mspt-took));\n" +
                "           start_time = time()\n" +
                "       )\n" +
                "   );\n" +
                "   game_tick(1000);\n" +
                "   'Done!'\n" +
                ");\n" +
                "\n" +
                " _show_path_tick(particle_type, total) -> (\n" +
                "   __assert_valid_for_motion();\n" +
                "   __prepare_path_if_needed();\n" +
                "   loop(total,\n" +
                "       segment = floor(rand(length(global_points)-1));\n" +
                "       start = element(element(global_points, segment),1);\n" +
                "       end = element(element(global_points, segment+1),1);\n" +
                "       index = floor(rand(end-start));\n" +
                "       l(x, y, z) = slice(__get_path_at(segment, start, index), 0, 3);\n" +
                "       particle(particle_type, x, y, z, 1, 0, 0)\n" +
                "   );\n" +
                "   null\n" +
                ");\n" +
                "\n" +
                " __prepare_path_if_needed_generic() ->\n" +
                "(\n" +
                "   if(!global_path_precalculated, __invalidate_points_cache())\n" +
                ");\n" +
                "\n" +
                "__find_position_for_linear(segment, point) ->\n" +
                "(\n" +
                "   l(va, start, mode_a) = element(global_points,segment);\n" +
                "   l(vb, end, mode_b)   = element(global_points,segment+1);\n" +
                "   section = end-start;\n" +
                "   dt = point/section;\n" +
                "   dt*vb+(1-dt)*va\n" +
                ");\n" +
                "\n" +
                "\n" +
                "'(1/sqrt(2*pi*d*d))*euler^(-((x-miu)^2)/(2*d*d)) ';\n" +
                " 'but we will be normalizing anyways, so who cares';\n" +
                " __norm_prob(x, miu, d) -> euler^(-((x-miu)^2)/(2*d*d));\n" +
                "\n" +
                " __find_position_for_gauss(from_index, point) -> \n" +
                "(\n" +
                "   dev = global_interpol_option;\n" +
                "   components = l();\n" +
                "   path_point = element(element(global_points, from_index),1);\n" +
                "   \n" +
                "   try(\n" +
                "       for(range(from_index+1, length(global_points)),\n" +
                "           l(v,ptime,mode) = element(global_points, _);\n" +
                "           dev = if (global_interpol_option > 0, global_interpol_option, \n" +
                "               devs = l();\n" +
                "               if (_+1 < length(global_points), devs += element(element(global_points, _+1),1)-ptime);\n" +
                "               if (_-1 >= 0, devs += ptime-element(element(global_points, _-1),1));\n" +
                "               0.6*reduce(devs, _a+_, 0)/length(devs)\n" +
                "           );\n" +
                "           impact = __norm_prob(path_point+point, ptime, dev);\n" +
                "           if(rtotal && impact < 0.000001*rtotal, throw(null));\n" +
                "           components += l(v, impact);\n" +
                "           rtotal += impact\n" +
                "       )\n" +
                "       ,null\n" +
                "   );\n" +
                "   try(\n" +
                "       for(range(from_index, -1, -1),\n" +
                "           l(v,ptime,mode) = element(global_points, _);\n" +
                "           dev = if (global_interpol_option > 0, global_interpol_option, \n" +
                "               devs = l();\n" +
                "               if (_+1 < length(global_points), devs += element(element(global_points, _+1),1)-ptime);\n" +
                "               if (_-1 >= 0, devs += ptime-element(element(global_points, _-1),1));\n" +
                "               0.6*reduce(devs, _a+_, 0)/length(devs)\n" +
                "           );\n" +
                "           impact = __norm_prob(path_point+point, ptime, dev);\n" +
                "           if(ltotal && impact < 0.000001*ltotal, throw(null));\n" +
                "           components += l(v, impact);\n" +
                "           ltotal += impact\n" +
                "       )\n" +
                "       ,null\n" +
                "   );\n" +
                "   total = rtotal+ltotal;\n" +
                "   reduce(components, _a+element(_,0)*(element(_,1)/total), l(0,0,0,0,0))\n" +
                ");\n" +
                " __prepare_path_if_needed_bungee(fps) ->\n" +
                "(\n" +
                "   exit('not ready!');\n" +
                "   if(!global_points, exit('Cannot show path that doesn\\'t exist'));\n" +
                "   if(length(global_points)<2, exit('Path not complete - add more points'));\n" +
                "   v = element(element(global_points, 0),0);\n" +
                "   modify(p, 'pos', slice(v,0,3));\n" +
                "   modify(p, 'yaw', element(v,3));\n" +
                "   modify(p, 'pitch', element(v,4));\n" +
                "   current_target = element(element(global_points, 1),0);\n" +
                "   current_hook = 1;\n" +
                "   loop(length(global_points)-1, \n" +
                "       current_base = _;\n" +
                "       l(va, start, mode_a) = element(global_points,_);\n" +
                "       l(vb, end, mode_b)   = element(global_points,_+1);\n" +
                "       section = end-start;\n" +
                "       loop(section,\n" +
                "           dt = _/section;\n" +
                "\n" +
                "           v = dt*vb+(1-dt)*va;\n" +
                "           modify(p, 'pos', slice(v,0,3));\n" +
                "           modify(p, 'yaw', element(v,3));\n" +
                "           modify(p, 'pitch', element(v,4));\n" +
                "           game_tick(1000/fps)\n" +
                "       )\n" +
                "   );\n" +
                "   game_tick(1000);\n" +
                "   'Done!'\n" +
                ")\n";
    }
}
