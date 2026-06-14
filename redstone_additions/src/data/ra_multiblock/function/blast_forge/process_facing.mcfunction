# /ra_multiblock:blast_forge/process_facing
# MACRO FUNCTION — Recipe matching + IO for one direction (heat-based system)
# Called: function ... with storage ra:multiblock bf_dir.{direction}
# Context: as multiblock marker, at base position
#
# Inputs:  in1 (material barrel)
# Outputs: out1 (result barrel)
# Fuel is consumed by consume_fuel.mcfunction (separate from processing)
# Recipes: defined in blast_forge/recipes/ subfolder

data remove storage ra:temp bf

# === Read material from input_1 ===
$data modify storage ra:temp bf.material set from block $(in1) Items[0]
execute unless data storage ra:temp bf.material run return 0

# === Recipe Matching (data-driven from recipes/ subfolder) ===
data remove storage ra:temp bf.result
function ra_multiblock:blast_forge/recipes/match_all
execute unless data storage ra:temp bf.result run return 0

# === Consume 1 material ===
$execute positioned $(in1) run function ra_multiblock:blast_forge/consume_first_item

# === Insert result into output barrel ===
$execute positioned $(out1) run function ra_lib:inventory/insert with storage ra:temp bf.result

# === Update status data on marker entity ===
execute store result entity @s data.status.heat int 1 run scoreboard players get @s ra.heat

# === Processing Effects ===
$particle minecraft:flame $(smoke) 0.2 0.2 0.2 0.02 10
playsound minecraft:block.furnace.fire_crackle block @a[distance=..8] ~ ~ ~ 0.5 1
playsound minecraft:block.lava.extinguish block @a[distance=..8] ~ ~ ~ 0.2 1.5
