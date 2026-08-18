# /ra_wires:fluid/drain_loose
# Empty one filled container lying loose on top of the drain.
# Context: as the drain marker, at the drain position.
#
# Separate from drain_above because a dropped stack is not a container: it has a
# count that has to be decremented rather than a slot to swap, and the empty it
# leaves behind has nowhere to go but the ground.

execute unless entity @e[type=item,distance=..2.5] run return 0

data remove storage ra:wires iq
data modify storage ra:wires iq.queue set from storage ra:wires item_sources
function ra_wires:fluid/drain_loose_next
