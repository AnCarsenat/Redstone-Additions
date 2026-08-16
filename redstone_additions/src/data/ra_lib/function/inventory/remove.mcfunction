# /ra_lib:inventory/remove {id:"minecraft:...",count:N}
# Remove N items with the given id from the container at the current position.
# Input: $(id) = item ID (e.g., "minecraft:wheat"), $(count) = quantity to remove
# Output: returns 1 if the full amount was removed, 0 otherwise
#
# Works with any container size and with the requested amount spread over
# several stacks. All-or-nothing: when the container holds less than the amount
# asked for, nothing is taken and the function returns 0.
#
# Example: function ra_lib:inventory/remove {id:"minecraft:wheat",count:1}

$data modify storage ra:inventory req.id set value "$(id)"
$scoreboard players set #inv_need ra.temp $(count)

execute if score #inv_need ra.temp matches ..0 run return 0
execute unless data block ~ ~ ~ Items run return 0

# Rebuild the item list into `out` one entry at a time, taking from matching
# stacks on the way. The container itself is untouched until the whole request
# has been satisfied, so a failed removal cannot consume a partial amount.
data modify storage ra:inventory scan set from block ~ ~ ~ Items
data modify storage ra:inventory out set value []
function ra_lib:inventory/remove/step with storage ra:inventory req
data remove storage ra:inventory scan

# Anything still outstanding means the container did not hold enough.
execute if score #inv_need ra.temp matches 1.. run data remove storage ra:inventory out
execute if score #inv_need ra.temp matches 1.. run return 0

data modify block ~ ~ ~ Items set from storage ra:inventory out
data remove storage ra:inventory out
return 1
