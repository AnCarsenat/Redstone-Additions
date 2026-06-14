# /ra_gates:blocks/uni_gate/process
# Process UNI gate logic. As armor stand, at position.

# Initialize gate_type if not set (default to AND)
execute unless data entity @s data.properties.gate_type run data modify entity @s data.properties.gate_type set value "and"

# Detect redstone state for this gate marker.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect
tag @s remove ra.redstone.ignore_blocks

# Clear output and transient input-state tags
tag @s remove ra.out_success
tag @s remove ra.gate.has_inactive_input

# Count active powered input directions.
scoreboard players set @s ra.temp 0
execute if score @s ra.power.north matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.south matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.east matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.west matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.up matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.down matches 1.. run scoreboard players add @s ra.temp 1

# Legacy-like inactive input detection (adjacent off dust/levers).
execute if block ~ ~ ~-1 minecraft:redstone_wire[power=0] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~ ~1 minecraft:redstone_wire[power=0] run tag @s add ra.gate.has_inactive_input
execute if block ~-1 ~ ~ minecraft:redstone_wire[power=0] run tag @s add ra.gate.has_inactive_input
execute if block ~1 ~ ~ minecraft:redstone_wire[power=0] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~1 ~ minecraft:redstone_wire[power=0] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~-1 ~ minecraft:redstone_wire[power=0] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~ ~-1 minecraft:lever[powered=false] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~ ~1 minecraft:lever[powered=false] run tag @s add ra.gate.has_inactive_input
execute if block ~-1 ~ ~ minecraft:lever[powered=false] run tag @s add ra.gate.has_inactive_input
execute if block ~1 ~ ~ minecraft:lever[powered=false] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~1 ~ minecraft:lever[powered=false] run tag @s add ra.gate.has_inactive_input
execute if block ~ ~-1 ~ minecraft:lever[powered=false] run tag @s add ra.gate.has_inactive_input

# AND: Output TRUE only if ALL inputs are powered and at least one input exists
execute if data entity @s data.properties{gate_type:"and"} if score @s ra.temp matches 1.. unless entity @s[tag=ra.gate.has_inactive_input] run tag @s add ra.out_success

# OR: Output TRUE if ANY input is powered
execute if data entity @s data.properties{gate_type:"or"} if score @s ra.temp matches 1.. run tag @s add ra.out_success

# NOT: Output TRUE if NO inputs are powered
execute if data entity @s data.properties{gate_type:"not"} if score @s ra.temp matches 0 run tag @s add ra.out_success

# XOR: Output TRUE if exactly ONE input is powered
execute if data entity @s data.properties{gate_type:"xor"} if score @s ra.temp matches 1 run tag @s add ra.out_success

# NAND: Output TRUE if NOT all inputs are powered (inverse of AND)
execute if data entity @s data.properties{gate_type:"nand"} unless score @s ra.temp matches 1.. run tag @s add ra.out_success
execute if data entity @s data.properties{gate_type:"nand"} if entity @s[tag=ra.gate.has_inactive_input] run tag @s add ra.out_success

# NOR: Output TRUE if NO inputs are powered (inverse of OR)
execute if data entity @s data.properties{gate_type:"nor"} if score @s ra.temp matches 0 run tag @s add ra.out_success

# XNOR: Output TRUE if NOT exactly one input is powered (inverse of XOR)
execute if data entity @s data.properties{gate_type:"xnor"} unless score @s ra.temp matches 1 run tag @s add ra.out_success

# Set output - replace nearby iron blocks with redstone blocks or vice versa
execute if entity @s[tag=ra.out_success] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute unless entity @s[tag=ra.out_success] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
