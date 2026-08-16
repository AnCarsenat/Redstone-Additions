# /ra_lib:inventory/insert {id:"minecraft:...",count:N,components:{...}}
# Insert item into container at current position using /loot insert.
# Handles stacking automatically via game mechanics.
#
# FALLBACK PATH. Prefer ra_lib:inventory/move_slot, which moves a whole stack
# with /item replace: no loot table to parse, and the stack crosses verbatim
# instead of being rebuilt from an id plus a components blob. Use this only when
# the destination has no free slot and the items have to merge into partial
# stacks, which /item cannot express.
#
# Caveat: whatever does not fit is not returned to the caller beyond the count in
# the result, so callers must not assume the whole amount moved.
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
