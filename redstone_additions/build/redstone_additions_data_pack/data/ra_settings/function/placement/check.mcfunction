# /ra_settings:placement/check
# May the block this bat is carrying be placed? Context: as the placement bat.
# Sets #blocked ra.set.tmp to 1 when the type is switched off.
#
# Only DISABLED types are walked, and the list is empty on a pack nobody has
# configured -- so the common case is a single "is the list empty" test and no
# iteration at all. A flag per block would instead cost one check per block type
# on every placement for ever.

scoreboard players set #blocked ra.set.tmp 0
execute unless data storage ra:settings disabled[0] run return 0

data modify storage ra:settings dscan set from storage ra:settings disabled
function ra_settings:placement/check_step
data remove storage ra:settings dscan
