# /ra_lib:skin/set_lit {id:"electric_furnace",lit:"true"}
# Flip an existing skin's `lit` state in place. Context: at the block.
#
# WHY THIS EXISTS RATHER THAN CALLING apply_lit AGAIN
# apply_lit kills the old display and summons a replacement. `kill` does not
# remove the entity until the end of the tick, so for the rest of that tick there
# are TWO block_displays at the same position with the same scale and the same
# depth -- which is z-fighting -- and the handover pops a frame where the skin is
# simply gone. A machine that changes lit state often does that every time it
# starts or stops, which is why the Electric Furnace flickered and vanished on
# the fast modes.
#
# The display is already there and already correctly oriented. Only one string
# needs to change, so change it on the entity that exists. No kill, no summon,
# no frame without a skin.
#
# Returns 1 if a display was found and updated, 0 if there was none -- the caller
# falls back to apply_lit to build one from nothing.

$execute align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.$(id),distance=..0.4,limit=1] run return 0
$execute align xyz positioned ~0.5 ~0.5 ~0.5 as @e[type=block_display,tag=ra.skin.$(id),distance=..0.4] run data modify entity @s block_state.Properties.lit set value "$(lit)"
return 1
