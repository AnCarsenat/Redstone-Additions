# /ra_lib:skin/clear {id:"unboxer"}
# Remove the skin at the current position. Call from the block's break handler,
# before the marker is killed.

$kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.9]
