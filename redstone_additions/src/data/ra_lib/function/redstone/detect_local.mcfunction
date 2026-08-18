# /ra_lib:redstone/detect_local
# ra_lib:redstone/detect, plus the look-space view of the same reading.
# As the marker, at the block. Returns ra.power.
#
# Adds on top of detect:
#   ra.power.front/back/left/right/local_up/local_down
#   ra.powered.{north,south,east,west,up,down}
#   ra.powered.{front,back,left,right,local_up,local_down}
#
# Split out from detect because nothing in the pack reads any of it, and making
# every redstone-reading block pay for twenty-two tag removals and six extra
# scores to maintain an unread view was most of what detect cost. It stays
# available because it is documented as part of the library's contract and an
# addon may well want it.
#
# If you are writing new code, prefer ra_lib:redstone/local/front and its five
# siblings: they read one named side for about a tenth of what this costs, and
# they do not need the whole reading computed first.

function ra_lib:redstone/clear
function ra_lib:redstone/detect

# Look-space mapping from the marker's own facing.
# ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
execute if score @s ra.facing matches 0 run function ra_lib:redstone/facing/down
execute if score @s ra.facing matches 1 run function ra_lib:redstone/facing/up
execute if score @s ra.facing matches 2 run function ra_lib:redstone/facing/north
execute if score @s ra.facing matches 3 run function ra_lib:redstone/facing/south
execute if score @s ra.facing matches 4 run function ra_lib:redstone/facing/west
execute if score @s ra.facing matches 5 run function ra_lib:redstone/facing/east

# Direction tags, only worth testing when something is powered at all.
execute if score @s ra.power matches 1.. run function ra_lib:redstone/tags

return run scoreboard players get @s ra.power
