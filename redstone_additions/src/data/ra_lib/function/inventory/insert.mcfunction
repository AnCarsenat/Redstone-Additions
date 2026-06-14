# /ra_lib:inventory/insert {id:"minecraft:...",count:N,components:{...}}
# Insert item into container at current position using /loot insert.
# Handles stacking automatically via game mechanics.
#
# Input: Macro with item fields directly:
#   $(id) = item ID (e.g., "minecraft:diamond")
#   $(count) = item count (e.g., 1)
#   $(components) = optional components (e.g., {} or {"minecraft:damage":{damage:10}})
# Output: returns number of items inserted (0 if container full)
#
# Example: function ra_lib:inventory/insert {id:"minecraft:diamond",count:1,components:{}}
# For storage: function ra_lib:inventory/insert with storage ra:temp pipe_item

$return run loot insert ~ ~ ~ loot {pools:[{rolls:1,entries:[{type:"item",name:"$(id)",functions:[{function:"set_count",count:$(count)},{function:"set_components",components:$(components)}]}]}]}
