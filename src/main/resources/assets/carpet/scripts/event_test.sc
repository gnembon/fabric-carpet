
__on_player_jumps(player) -> print('Player '+player+' jumps.');

__on_player_deploys_elytra(player) -> print('Player '+player+' deploys elytra.');

__on_player_wakes_up(player) -> print('Player '+player+' wakes up. Lemme know when this starts working');

__on_player_rides(player, forward, strafe, jumping, sneaking) ->
(
	print('');
	print('player rides a vehicle:');
	print('  - player: '+player);
	print('  - forward/backward: '+forward);
	print('  - strafing: '+strafe);
	print('  - jumping: '+jumping);
	print('  - sneaking: '+sneaking)
);

__on_player_uses_item(player, item_tuple, hand) ->
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('player uses an item:');
	print('  - player: '+player);
	print('  - item:');
	print('    > name: '+item);
	print('    > count: '+count);
	print('    > nbt: '+nbt);
	print('  - hand: '+hand)
);

__on_player_clicks_block(player, block, face) ->
(
	print('');
	print('player clicks an item:');
	print('  - player: '+player);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)));
	print('  - face: '+face)
);


__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) ->
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('block right clicked by player:');
	print('  - player: '+player);
	print('  - item: '+item);
	print('  - hand: '+hand);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)));
	print('  - face: '+face);
	print('  - hitvec: '+map(hitvec, str('%.2f',_)))
);

__on_player_breaks_block(player, block) ->
(
	print('');
	print('player breaks block:');
	print('  - player: '+player);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)))
);

__on_player_interacts_with_entity(player, entity, hand) ->
(
	print('');
	print('player interacts with entity:');
	print('  - player: '+player);
	print('  - entity: '+entity+' at '+map(pos(entity), str('%.2f',_)));
	print('  - hand: '+hand)
);

__on_player_attacks_entity(player, entity) ->
(
	print('');
	print('player attacks entity:');
	print('  - player: '+player);
	print('  - entity: '+entity+' at '+map(pos(entity), str('%.2f',_)))
);

__on_player_starts_sneaking(player) -> print('Player '+player+' starts sneaking.');

__on_player_stops_sneaking(player) -> print('Player '+player+' stops sneaking.');

__on_player_starts_sprinting(player) -> print('Player '+player+' starts sprinting.');

__on_player_stops_sprinting(player) -> print('Player '+player+' stops sprinting.');

__on_player_releases_item(player, item_tuple, hand) -> 
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('player releases an item:');
	print('  - player: '+player);
	print('  - item:');
	print('    > name: '+item);
	print('    > count: '+count);
	print('    > nbt: '+nbt);
	print('  - hand: '+hand)
);

__on_player_finishes_using_item(player, item_tuple, hand) -> 
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('player finishes using an item:');
	print('  - player: '+player);
	print('  - item:');
	print('    > name: '+item);
	print('    > count: '+count);
	print('    > nbt: '+nbt);
	print('  - hand: '+hand)
);

__on_player_drops_item(player) -> print('Player '+player+' drops current item.');

__on_player_drops_stack(player) -> print('Player '+player+' drops current stack.');
