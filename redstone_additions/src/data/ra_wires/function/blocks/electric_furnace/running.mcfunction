# /ra_wires:blocks/electric_furnace/running
# One item just finished. Context: as the marker, at the block.
#
# The look is stay_lit's job and is shared with `cooking`; what belongs here is
# only what marks the completion itself -- a puff and a crackle per item, not per
# tick, so superpowered does not turn into a smoke column and a wall of sound.

function ra_wires:blocks/electric_furnace/stay_lit

particle minecraft:smoke ~ ~1.1 ~ 0.15 0.05 0.15 0.01 2
particle minecraft:flame ~ ~0.6 ~ 0.2 0.1 0.2 0.005 1
playsound minecraft:block.blastfurnace.fire_crackle block @a[distance=..10] ~ ~ ~ 0.3 1.6
