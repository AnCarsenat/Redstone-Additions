# /ra_lib:skin/apply {real:"minecraft:barrel",skin:"minecraft:dispenser",id:"unboxer"}
# Draw `skin` over the `real` block at the current position.
# Context: at the block position.
#
# WHY THIS EXISTS
# Some vanilla blocks carry behaviour you cannot switch off. A dispenser fires
# its own inventory on any rising redstone edge; a dropper does the same. A
# custom block that stores items in itself and sits in a redstone build will
# therefore eject them, and no amount of datapack logic can intercept it.
#
# The fix is to place a block whose behaviour you DO want — a barrel gives the
# same inventory and GUI with no dispense — and put the appearance back with a
# block_display. Mechanics and looks stop being the same decision.
#
# The facing is read back off the real block rather than from stored state, so a
# block rotated by any means still gets a skin pointing the same way, and a skin
# that drifts out of sync repairs itself the next time this runs.
#
# For blocks with no `facing` property, use ra_lib:skin/apply_static.

# Both kills are anchored to the block centre, where ra_lib:skin/spawn now stands
# its displays, so our own skin is at distance 0 and the neighbour's at a full 1.0.
# Any radius under 0.5 separates them cleanly; 0.4 leaves room for a caller that
# is not exactly centred.
$execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]

# Migration for worlds built before skins moved to the centre. Old skins stand on
# the block's minimum corner, which the centre-anchored kill above cannot reach —
# without this line the stale one would sit there forever with a new skin drawn
# on top of it. Anchored to the corner and just as narrow, so it cannot reach a
# neighbour's skin of either vintage. Safe to delete once no old worlds remain.
$execute align xyz run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]

$execute unless block ~ ~ ~ $(real) run return 0

$execute if block ~ ~ ~ $(real)[facing=north] run function ra_lib:skin/spawn {skin:"$(skin)",id:"$(id)",facing:"north"}
$execute if block ~ ~ ~ $(real)[facing=south] run function ra_lib:skin/spawn {skin:"$(skin)",id:"$(id)",facing:"south"}
$execute if block ~ ~ ~ $(real)[facing=east] run function ra_lib:skin/spawn {skin:"$(skin)",id:"$(id)",facing:"east"}
$execute if block ~ ~ ~ $(real)[facing=west] run function ra_lib:skin/spawn {skin:"$(skin)",id:"$(id)",facing:"west"}
$execute if block ~ ~ ~ $(real)[facing=up] run function ra_lib:skin/spawn {skin:"$(skin)",id:"$(id)",facing:"up"}
$execute if block ~ ~ ~ $(real)[facing=down] run function ra_lib:skin/spawn {skin:"$(skin)",id:"$(id)",facing:"down"}

return 1
