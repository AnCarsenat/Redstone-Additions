# /ra_lib:transport/rebuild/snapshot_read {id:N}
# Internal: copy the network's contents onto the root node, so they survive the
# rebuild that is about to discard every network.
#
# THE WHOLE BREAKDOWN TRAVELS, NOT JUST THE TOTAL
# This used to park `amount` and `medium` and nothing else. A rebuild therefore
# turned a run holding 5000 water and 5000 lava into one holding 10000 of
# whatever `medium` happened to name, with an EMPTY `amounts` map -- and since
# reset_net writes `media:[]`, the read-time migration never fired to repair it
# either. The next thing offered to that network became media[0] and so became
# the name on the goggles: pump 5000 water, place a block, pump 5000 lava, and
# the run reports 10000 Lava.
#
# So the per-medium figures are carried too, as a list of {m,a} on the marker,
# and ra.tr.carry is summed from them rather than read off `amount`. Summing it
# is what keeps the total and the breakdown from disagreeing: whatever absorb
# puts back per medium is exactly what accumulate adds to the total.

# A network written before multi-medium has no breakdown to carry. Give it one
# first, with the same code the read path uses, rather than a second dialect of
# the same migration here.
$execute if data storage ra:transport nets.n$(id).medium unless data storage ra:transport nets.n$(id).media[0] run function ra_lib:transport/net/migrate_pick {id:$(id)}

scoreboard players set @s ra.tr.carry 0
data remove entity @s data.data.carry
data remove entity @s data.data.carry_potion

# Cleared before it is written, not merely overwritten when there is something to
# write. A root whose network is empty would otherwise keep the name of whatever
# it held last, for ever -- and rebuild/absorb hands that stale name to
# adopt_medium on the one path that carries a total with no breakdown behind it,
# which names a network after a medium it has never held.
data remove entity @s data.data.medium

$execute if data storage ra:transport nets.n$(id).medium run data modify entity @s data.data.medium set from storage ra:transport nets.n$(id).medium

# A Potion network's effect list belongs to the network, not to any node, so it
# went the same way the breakdown did on every rebuild.
$execute if data storage ra:transport nets.n$(id).potion run data modify entity @s data.data.carry_potion set from storage ra:transport nets.n$(id).potion

# NOTHING TRAVELS THAT CANNOT BE NAMED
# This used to carry the bare `amount` on, on the grounds that losing it silently
# was worse than losing its name. It is worse. A total with no breakdown behind
# it is indestructible: the next rebuild carries it again, `amounts` stays empty,
# and the first medium offered afterwards becomes the only entry in `media` and
# therefore the name of the whole total -- so 15000 mL of nothing reports itself
# as 15000 mL of the next thing you pour in. Three networks in a test world ended
# up holding a five-figure amount with no medium at all and no way back, because
# both migrations are gated on a `medium` that those networks did not have.
#
# So the invariant is that `amount` is exactly the sum of `amounts`, and it is
# enforced by the only thing that can enforce it: a rebuild carries the breakdown
# and nothing else, and rebuild/accumulate sums the total back from what was
# carried. An amount with no breakdown is not contents, it is a bookkeeping
# error, and it goes.
$execute unless data storage ra:transport nets.n$(id).media[0] run return 0

# Created here rather than above, so the branch that returns without walking does
# not leave an empty list behind on the marker for ever.
data modify entity @s data.data.carry set value []

data remove storage ra:transport snapq
$data modify storage ra:transport snapq set value {id:$(id)}
$data modify storage ra:transport snapq.queue set from storage ra:transport nets.n$(id).media
function ra_lib:transport/rebuild/snapshot_next

execute unless data entity @s data.data.carry[0] run data remove entity @s data.data.carry
