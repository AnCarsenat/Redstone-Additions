# /ra_lib:transport/net/take_any {amount:N}
# Take from whichever medium the network calls its primary, and leave the name of
# what was taken in storage ra:transport took.medium.
# Context: as a node marker. Returns how much was removed.
#
# For the callers that move contents around without caring what they are -- a
# Valve, a Breaker, an Ender Fluid Vault. They still have to know afterwards
# WHICH medium they are carrying, so that the far side is given the same thing,
# which is why the name is parked in storage rather than thrown away.

data remove storage ra:transport took
scoreboard players set #net_moved ra.tr.tmp 0
execute if score @s ra.tr.net matches ..0 run return 0

function ra_lib:transport/net/read
execute unless data storage ra:transport cur.medium run return 0

data modify storage ra:transport took.medium set from storage ra:transport cur.medium
$data modify storage ra:transport took.amount set value $(amount)
return run function ra_lib:transport/net/take with storage ra:transport took
