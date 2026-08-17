# /ra_ender:link/hand_over {x,y,z}
# Give this barrel's whole contents to the barrel at x y z.
# Context: as the holding vault marker, at its barrel.
#
# Copy first, and only remove if the copy reported success: the stacks exist in
# one barrel before the pair of commands and in one barrel after, never in both
# and never in neither.

$execute store success score #ender.copied ra.temp run data modify block $(x) $(y) $(z) Items set from block ~ ~ ~ Items
execute if score #ender.copied ra.temp matches 0 run return 0

data remove block ~ ~ ~ Items

# Both ends have just changed on purpose; record that so the demand-driven modes
# do not read it as a player insert or extraction.
function ra_ender:blocks/item_vault/mark
playsound minecraft:entity.enderman.teleport block @a[distance=..10] ~ ~ ~ 0.4 1.4
particle minecraft:portal ~ ~1 ~ 0.25 0.3 0.25 0.1 12
