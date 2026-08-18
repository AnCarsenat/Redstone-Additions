# /ra_migrations:5.1.11-to-5.1.12
# Clear every existing skin so it is redrawn with the wider anti-z-fighting margin.
#
# 5.1.12 widened the skin overlay from 1.004 to 1.02 -- two thousandths of a block
# of clearance to a hundredth -- because the old margin was geometrically clear of
# the block underneath but too close for the depth buffer to separate at range, so
# skins flickered.
#
# Displays already in the world keep whatever transformation they were summoned
# with; nothing rewrites it. Killing them is the update: every skinned block
# checks each tick whether its skin is missing and redraws it if so, which is
# verified to cover all four of them -- Breeder, EU Generator, Electric Furnace
# and Unboxer -- so the gap lasts one tick.
#
# Safe to run more than once, which it will be: it costs one redraw per skinned
# block per load.

kill @e[type=block_display,tag=ra.skin]
