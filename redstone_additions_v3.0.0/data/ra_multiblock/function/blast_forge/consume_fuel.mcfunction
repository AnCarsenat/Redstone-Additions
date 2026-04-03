# /ra_multiblock:blast_forge/consume_fuel
# MACRO FUNCTION — Check fuel barrel and consume fuel to add heat
# Called: function ... with storage ra:multiblock bf_dir.{direction}
# Context: as multiblock marker, at base position
#
# Fuel tiers:
#   Coal        → +50 heat
#   Charcoal    → +40 heat
#   Blaze Powder → +500 heat
#   Blaze Rod   → +1500 heat  (NEW in v4.0.1 — note: not a standard blast furnace fuel)
#   Lava Bucket → +5000 heat  (NEW in v4.0.1 — returns empty bucket)
#
# Only consumes fuel if there are materials to process in input_1

# === Check if there are materials to process ===
$execute unless data block $(in1) Items[0] run return 0

# === Read fuel from input_2 ===
$data modify storage ra:temp bf.fuel_check set from block $(in2) Items[0]
execute unless data storage ra:temp bf.fuel_check run return 0

# === Match fuel type and add heat ===

# Coal → +50 heat
execute if data storage ra:temp bf.fuel_check{id:"minecraft:coal"} run scoreboard players add @s ra.heat 50
$execute if data storage ra:temp bf.fuel_check{id:"minecraft:coal"} positioned $(in2) run function ra_multiblock:blast_forge/consume_first_item
execute if data storage ra:temp bf.fuel_check{id:"minecraft:coal"} run return 1

# Charcoal → +40 heat
execute if data storage ra:temp bf.fuel_check{id:"minecraft:charcoal"} run scoreboard players add @s ra.heat 40
$execute if data storage ra:temp bf.fuel_check{id:"minecraft:charcoal"} positioned $(in2) run function ra_multiblock:blast_forge/consume_first_item
execute if data storage ra:temp bf.fuel_check{id:"minecraft:charcoal"} run return 1

# Blaze Powder → +500 heat
execute if data storage ra:temp bf.fuel_check{id:"minecraft:blaze_powder"} run scoreboard players add @s ra.heat 500
$execute if data storage ra:temp bf.fuel_check{id:"minecraft:blaze_powder"} positioned $(in2) run function ra_multiblock:blast_forge/consume_first_item
execute if data storage ra:temp bf.fuel_check{id:"minecraft:blaze_powder"} run return 1

# Blaze Rod → +1500 heat
execute if data storage ra:temp bf.fuel_check{id:"minecraft:blaze_rod"} run scoreboard players add @s ra.heat 1500
$execute if data storage ra:temp bf.fuel_check{id:"minecraft:blaze_rod"} positioned $(in2) run function ra_multiblock:blast_forge/consume_first_item
execute if data storage ra:temp bf.fuel_check{id:"minecraft:blaze_rod"} run return 1

# Lava Bucket → +5000 heat (replaces with empty bucket)
execute if data storage ra:temp bf.fuel_check{id:"minecraft:lava_bucket"} run scoreboard players add @s ra.heat 5000
$execute if data storage ra:temp bf.fuel_check{id:"minecraft:lava_bucket"} positioned $(in2) run data modify block ~ ~ ~ Items[0].id set value "minecraft:bucket"
$execute if data storage ra:temp bf.fuel_check{id:"minecraft:lava_bucket"} positioned $(in2) run data modify block ~ ~ ~ Items[0].count set value 1
execute if data storage ra:temp bf.fuel_check{id:"minecraft:lava_bucket"} run return 1

# === Cap heat at 1000 ===
execute if score @s ra.heat matches 1001.. run scoreboard players set @s ra.heat 1000

# Clean up temp
data remove storage ra:temp bf.fuel_check

# No valid fuel found
return 0
