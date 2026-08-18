# /ra_jetpacks:flight/scorch_particles
# The Scorch kit's exhaust. Context: at the flying player.
#
# Clean fire, spread wide. An earlier version threw about a hundred particles a
# tick including `explosion` billows and `large_smoke`, which read as a soot
# cloud rather than a jet -- the grey swamped the orange and there was enough of
# it to hide your own boots. `lava` is gone too: the popping blobs read as
# dripping stone, not flame.
#
# So: flame and small_flame only, and the width comes from the spread arguments
# rather than from the count. A wide, thin plume looks like thrust; a narrow
# dense one looks like a campfire.

particle minecraft:flame ~ ~-0.3 ~ 0.55 0.25 0.55 0.04 10
particle minecraft:small_flame ~ ~-0.5 ~ 0.65 0.4 0.65 0.04 8

# Down the column it is burning, so the six block reach is visible rather than
# inferred from something catching fire. Widening with depth, the way an exhaust
# plume actually spreads.
particle minecraft:flame ~ ~-2.0 ~ 0.75 0.8 0.75 0.02 7
particle minecraft:small_flame ~ ~-4.5 ~ 0.95 1.2 0.95 0.02 6
