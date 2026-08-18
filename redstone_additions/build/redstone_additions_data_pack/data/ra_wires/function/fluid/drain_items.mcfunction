# /ra_wires:fluid/drain_items
# A vertical drain empties what a player is holding into the network.
# Context: as the drain marker, at the drain position.
#
# WHY VERTICAL
# A drain lying flat is working on the world: it takes a source block beside it,
# or puts one back. Stood on end it has nothing sensible to do with the block
# above or below, so that placement is given the other job — it becomes the point
# where a base's contents go in by hand. One block, two roles, decided by how you
# place it, with no extra property to discover.
#
# Sneak while holding the container, standing on or beside the drain. Sneaking is
# the gate because a bucket is also how you pick fluid up: without it, walking
# past a drain with a full bucket would quietly empty it.

execute unless entity @a[distance=..2.5,predicate=ra:is_sneaking,limit=1] run return run data modify entity @s data.status.drain_state set value "waiting_for_hand"

data remove storage ra:wires iq
data modify storage ra:wires iq.queue set from storage ra:wires item_sources
scoreboard players set #dr.took ra.wires.tmp 0

function ra_wires:fluid/drain_item_next

# Nothing in hand to empty, so fall through to the other thing a player standing
# on a vertical drain can give it: their own experience.
execute if score #dr.took ra.wires.tmp matches 0 run function ra_wires:fluid/drain_exp
execute if score #dr.took ra.wires.tmp matches 0 unless data entity @s data.status{drain_state:"absorbing_xp"} run data modify entity @s data.status.drain_state set value "nothing_to_empty"
data remove storage ra:wires iq
