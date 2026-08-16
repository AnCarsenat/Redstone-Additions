# /ra_lib_multiblock:init
# Initialize multiblock library - called from ra:load

# Scoreboards for multiblock system
scoreboard objectives add ra.multiblock dummy
scoreboard objectives add ra.mb_timer dummy

# Storage namespace for multiblock data
data merge storage ra:multiblock {}
data merge storage ra:temp {bf:{}}

# Constant used by the rotation maths.
scoreboard players set #mb_neg ra.multiblock -1

# --- Type registry ---
# Rebuilt from scratch on every load so a removed or edited type cannot linger.
# A module registers its structures from #ra_lib_multiblock:load; everything the
# library derives from them (rotations, IO maps) is regenerated right after.
data modify storage ra:multiblock registry set value []
data modify storage ra:multiblock types set value {}

function #ra_lib_multiblock:load

function ra_lib_multiblock:build/all
