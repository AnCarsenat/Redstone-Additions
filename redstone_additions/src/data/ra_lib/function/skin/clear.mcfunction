# /ra_lib:skin/clear {id:"unboxer"}
# Remove the skin at the current position. Call from the block's break handler,
# before the marker is killed.

# Centre-anchored kill, plus the corner-anchored one that clears pre-centre
# skins; see ra_lib:skin/apply.
$execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
$execute align xyz run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
