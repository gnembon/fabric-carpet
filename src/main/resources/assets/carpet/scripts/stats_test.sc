__on_statistic(player, category, event, value) ->
(
    print('__on_statistic(player, category, event, value) -> ');
    print('   ['+join(', ',l(player, category, event, value))+']');
);