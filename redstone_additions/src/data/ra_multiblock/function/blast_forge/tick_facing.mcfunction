# /ra_multiblock:blast_forge/tick_facing
# MACRO FUNCTION — Per-tick logic for one direction (heat-based system)
# Called: function ... with storage ra:multiblock bf_dir.{direction}
# Context: as multiblock marker, at base position (already checked enabled)

# === Redstone Control ===
# Redstone block behind base = forge is locked (like hopper locking)
$execute if block $(ctrl) minecraft:redstone_block run return 0

# === Heat Decay ===
# Heat decreases by 1 every 2 ticks (passive cooling)
scoreboard players add @s ra.mb_timer 1
execute if score @s ra.mb_timer matches 2.. run scoreboard players remove @s ra.heat 1
execute if score @s ra.mb_timer matches 2.. run scoreboard players set @s ra.mb_timer 0

# Clamp heat to minimum 0
execute if score @s ra.heat matches ..-1 run scoreboard players set @s ra.heat 0

# === Visual Feedback — particle intensity scales with heat ===
# Low heat (1-249): light smoke
$execute if score @s ra.heat matches 1..249 run particle minecraft:smoke $(smoke) 1 1 1 0.003 1
# Medium heat (250-499): smoke + occasional flame
$execute if score @s ra.heat matches 250..499 run particle minecraft:smoke $(smoke) 1 1 1 0.005 2
$execute if score @s ra.heat matches 250..499 run particle minecraft:flame $(smoke) 1 1 1 0.005 1
# High heat (500-749): dense smoke + flames
$execute if score @s ra.heat matches 500..749 run particle minecraft:smoke $(smoke) 2 2 2 0.01 2
$execute if score @s ra.heat matches 500..749 run particle minecraft:flame $(smoke) 1 1 1 0.01 2
# Max heat (750-1000): flames + lava drips
$execute if score @s ra.heat matches 750.. run particle minecraft:flame $(smoke) 2 2 2 0.015 2
$execute if score @s ra.heat matches 750.. run particle minecraft:lava $(smoke) 1 1 1 0.005 1

# === Fuel Consumption ===
# Consume fuel to add heat only when heat < 1000 and there are materials to process
$execute if score @s ra.heat matches ..999 run function ra_multiblock:blast_forge/consume_fuel with storage ra:multiblock bf_dir.$(facing)

# === Processing ===
# Only process when heat >= 100
execute unless score @s ra.heat matches 100.. run return 0

# Processing speed scales with heat:
#   heat 100-299: process every 100 ticks (5 sec)
#   heat 300-599: process every 60 ticks (3 sec)
#   heat 600-899: process every 40 ticks (2 sec)
#   heat 900+:    process every 20 ticks (1 sec)
# Use a separate timer (#bf_process_timer) stored on the entity
scoreboard players add @s ra.cooldown 1

execute if score @s ra.heat matches 100..299 unless score @s ra.cooldown matches 100.. run return 0
execute if score @s ra.heat matches 300..599 unless score @s ra.cooldown matches 60.. run return 0
execute if score @s ra.heat matches 600..899 unless score @s ra.cooldown matches 40.. run return 0
execute if score @s ra.heat matches 900.. unless score @s ra.cooldown matches 20.. run return 0

scoreboard players set @s ra.cooldown 0

# Dispatch to recipe processing with same direction data
$function ra_multiblock:blast_forge/process_facing with storage ra:multiblock bf_dir.$(facing)
