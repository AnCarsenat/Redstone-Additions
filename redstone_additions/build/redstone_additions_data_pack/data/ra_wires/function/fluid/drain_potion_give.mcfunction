# /ra_wires:fluid/drain_potion_give {id:"minecraft:strength",secs:N,amp:N}
# Internal: hand one scaled effect to everyone standing by the drain.
# Context: as the drain marker, at the drain position.
#
# Four blocks, which is about arm's reach of the block plus the space in front of
# it -- close enough that standing there is a decision.

$effect give @a[distance=..4] $(id) $(secs) $(amp) false
