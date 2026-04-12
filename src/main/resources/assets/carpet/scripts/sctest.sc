__config() -> {
    'commands' -> {
        'shapes <duration>' -> 'test_shapes'
    },
    'arguments' -> {
         'duration' -> {'type' -> 'int', 'min' -> 0, 'max' -> 1000, 'suggest' -> [100]},
    }
};


test_shapes(duration) -> (
   [x, y, z] = pos(player());
   set(x, y, z, 'glass');
   set(x+10, y+10, z+10, 'glass');
   schedule(duration, _(outer(x), outer(y), outer(z)) -> (
       set(x, y, z, 'air');
       set(x+10, y+10, z+10, 'air');
   ));
   loop(10,
      draw_shape('line', duration, 'from', [x, y, z], 'to', [x-_, y+10, z+10], 'color', 0xff0000ff);
      draw_shape('line', duration, 'from', [x, y, z], 'to', [x-10, y+_, z+10], 'color', 0x00ff00ff);
      draw_shape('line', duration, 'from', [x, y, z], 'to', [x-10, y+10, z+_], 'color', 0x0000ffff);
   );
   loop(20,
      sq = _ * _ / 10;
      draw_shape('box', duration, 'from', [x+_, y + sq, z], 'to', [x+_ + 2, y+sq + 2, z+2], 'color', 0xff0000ff, 'fill', 0xff000030);
      draw_shape('box', duration, 'from', [x, y + _, z + sq], 'to', [x+2, y+_ + 2, z+sq + 2], 'color', 0x00ff00ff, 'fill', 0x00ff0030);
      draw_shape('box', duration, 'from', [x + sq, y, z + _], 'to', [x+sq + 2, y+2, z+_ + 2], 'color', 0x0000ffff, 'fill', 0x0000ff30);
   );
);