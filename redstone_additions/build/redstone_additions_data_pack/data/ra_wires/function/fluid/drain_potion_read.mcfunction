# /ra_wires:fluid/drain_potion_read {id:N,got:N}
# Internal: work out which effects this network's potion carries, and hand each
# one to drain_potion_apply.
#
# A potion brewed with commands carries `custom_effects` on the item, which is
# authoritative and needs no table. A preset potion carries only its id, so its
# effects come out of ra_wires:media/potions. Custom first, because a potion can
# carry both and the explicit list is the one that means something.

$execute unless data storage ra:transport nets.n$(id).potion run return 0

data remove storage ra:wires eff
$data modify storage ra:wires eff.list set from storage ra:transport nets.n$(id).potion.custom_effects
$execute unless data storage ra:wires eff.list run function ra_wires:fluid/drain_potion_preset {id:$(id)}
execute unless data storage ra:wires eff.list[0] run return 0

$scoreboard players set #pt.vol ra.wires.tmp $(got)
function ra_wires:fluid/drain_potion_next
