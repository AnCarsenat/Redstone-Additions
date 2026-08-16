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
# Caveat: whatever does not fit is DESTROYED, and the return value cannot be used
# to work out how much that was. `loot insert` reports the number of item entries
# it handled, not the number of items, so a 64-stack reports far less than 64 even
# when all of it went in. insert_or_drop originally subtracted this from the
# requested count and dropped the difference — inserting the stack AND dropping
# almost all of it again.
#
# Only safe with a count of 1, where "handled" and "inserted" coincide and the
# result is a plain success flag. For anything larger use insert_or_drop, which
# picks a slot explicitly and never consults a loot table.
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
