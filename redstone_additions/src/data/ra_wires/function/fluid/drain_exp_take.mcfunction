# /ra_wires:fluid/drain_exp_take {points:N}
# Internal: take the points off the player who is paying them.
# Context: as the drain marker.

$xp add @a[tag=ra.wires.xp_giver,limit=1] -$(points) points
