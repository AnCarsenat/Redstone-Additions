# /ra_wires:fluid/drain_items
# A vertical drain takes filled containers into the network.
# Context: as the drain marker, at the drain position.
#
# WHY VERTICAL
# A drain lying flat is working on the world: it takes a source block beside it,
# or puts one back. Stood on end it has nothing sensible to do with the block
# above or below, so that placement is given the other job — it becomes the point
# where a base's contents go in. One block, two roles, decided by how you place
# it, with no extra property to discover.
#
# THREE WAYS IN, TRIED IN THAT ORDER
#   1. a sneaking player's main hand
#   2. a container sitting on top of the drain
#   3. loose item entities on top of the drain
#
# Only the first needs sneaking. A bucket is also how you pick fluid UP, so
# without that gate, walking past a drain with a full bucket would quietly empty
# it. Nothing you have deliberately put in a barrel or dropped on the block is
# ambiguous in that way, which is why the other two run unattended — and being
# able to run unattended is the whole point of them: a hopper feeding buckets
# into a barrel over a drain is how a base loads a network without a player
# standing there.
#
# This used to return here when nobody was sneaking nearby, which is why a barrel
# of water buckets sat on top of a drain did nothing at all.

scoreboard players set #dr.took ra.wires.tmp 0

data remove storage ra:wires iq
data modify storage ra:wires iq.queue set from storage ra:wires item_sources
execute if entity @a[distance=..2.5,predicate=ra:is_sneaking,limit=1] run function ra_wires:fluid/drain_item_next

execute if score #dr.took ra.wires.tmp matches 0 run function ra_wires:fluid/drain_above
execute if score #dr.took ra.wires.tmp matches 0 run function ra_wires:fluid/drain_loose

# Nothing to empty, so fall through to the other thing a player standing on a
# vertical drain can give it: their own experience.
execute if score #dr.took ra.wires.tmp matches 0 run function ra_wires:fluid/drain_exp

execute if score #dr.took ra.wires.tmp matches 0 unless data entity @s data.status{drain_state:"absorbing_xp"} unless data entity @s data.status{drain_state:"network_full"} unless data entity @s data.status{drain_state:"wrong_medium"} run data modify entity @s data.status.drain_state set value "nothing_to_empty"
data remove storage ra:wires iq
