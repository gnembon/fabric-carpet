count_blocks(x1, y1, z1, x2, y2, z2) -> (
    volume(x1, y1, z1, x2, y2, z2,
        var('block_'+_) += 1
    );
    counts = map(vars('block_'), 
        total+= var(_);
        l(_ - 'block_', var(_))
    );
    for ( sort_key(counts, - element(_, 1) ),
        l(block, count) = _ ;
        print(str('%s: %d blocks, %.2f%%', block, count, 100*count/total))
    );
    'Total: '+total
)