# /ra_lib_multiblock:init
# Initialize multiblock library - called from ra:load

# Scoreboards for multiblock system
scoreboard objectives add ra.multiblock dummy
scoreboard objectives add ra.mb_timer dummy
scoreboard objectives add ra.mb_enabled dummy

# Storage namespace for multiblock data
data merge storage ra:multiblock {}
data merge storage ra:temp {bf:{}}
