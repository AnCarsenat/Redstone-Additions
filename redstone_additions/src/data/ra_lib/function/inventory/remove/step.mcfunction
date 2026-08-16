# /ra_lib:inventory/remove/step {id:"..."}
# Internal helper for ra_lib:inventory/remove.
# Moves storage ra:inventory scan[0] over to `out`, taking from it first when it
# matches the requested id, then recurses on the rest of the list.
#
# Every read targets scan[0] specifically, never a filtered path, so a container
# holding several stacks of the same item can never resolve ambiguously.

execute unless data storage ra:inventory scan[0] run return 0

scoreboard players set #inv_match ra.temp 0
$execute if data storage ra:inventory scan[0]{id:"$(id)"} if score #inv_need ra.temp matches 1.. run scoreboard players set #inv_match ra.temp 1

execute if score #inv_match ra.temp matches 0 run function ra_lib:inventory/remove/keep
execute if score #inv_match ra.temp matches 1 run function ra_lib:inventory/remove/consume

data remove storage ra:inventory scan[0]
function ra_lib:inventory/remove/step with storage ra:inventory req
