# /ra_gates:blocks/uni_gate/process
# Process UNI gate logic. As armor stand, at position.

# Initialize gate_type if not set (default to AND)
execute unless data entity @s data.properties.gate_type run data modify entity @s data.properties.gate_type set value "and"

# Detect redstone state for this gate marker. ignore_blocks stays on for the
# input census too, otherwise the gate's own redstone_block output shell would
# read back as six extra inputs.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect

# AND and NAND are the only modes that need to tell "input present but off"
# apart from "no input on that side", so the census only runs for them.
scoreboard players set @s ra.rs_inputs 0
execute if data entity @s data.properties{gate_type:"and"} run function ra_lib:redstone/count_inputs
execute if data entity @s data.properties{gate_type:"nand"} run function ra_lib:redstone/count_inputs

tag @s remove ra.redstone.ignore_blocks

# Clear output flag
tag @s remove ra.out_success

# Count powered input directions.
scoreboard players set @s ra.temp 0
execute if score @s ra.power.north matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.south matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.east matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.west matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.up matches 1.. run scoreboard players add @s ra.temp 1
execute if score @s ra.power.down matches 1.. run scoreboard players add @s ra.temp 1

# AND: at least one input, and every input that exists is powered.
# Comparing the powered count against the census covers unpowered repeaters,
# comparators, torches and buttons, not just dust and levers.
execute if data entity @s data.properties{gate_type:"and"} if score @s ra.temp matches 1.. if score @s ra.temp = @s ra.rs_inputs run tag @s add ra.out_success

# OR: Output TRUE if ANY input is powered
execute if data entity @s data.properties{gate_type:"or"} if score @s ra.temp matches 1.. run tag @s add ra.out_success

# NOT: Output TRUE if NO inputs are powered
execute if data entity @s data.properties{gate_type:"not"} if score @s ra.temp matches 0 run tag @s add ra.out_success

# XOR: Output TRUE if exactly ONE input is powered
execute if data entity @s data.properties{gate_type:"xor"} if score @s ra.temp matches 1 run tag @s add ra.out_success

# NAND: inverse of AND — true when nothing is powered, or when some existing
# input is not powered.
execute if data entity @s data.properties{gate_type:"nand"} if score @s ra.temp matches 0 run tag @s add ra.out_success
execute if data entity @s data.properties{gate_type:"nand"} unless score @s ra.temp = @s ra.rs_inputs run tag @s add ra.out_success

# NOR: Output TRUE if NO inputs are powered (inverse of OR)
execute if data entity @s data.properties{gate_type:"nor"} if score @s ra.temp matches 0 run tag @s add ra.out_success

# XNOR: Output TRUE if NOT exactly one input is powered (inverse of XOR)
execute if data entity @s data.properties{gate_type:"xnor"} unless score @s ra.temp matches 1 run tag @s add ra.out_success

# Set output. The fill rewrites a 3x3x3 volume and triggers block updates, so
# running it every tick cost 27 block writes per gate per tick even while the
# output was standing still. Write only when the result actually changes.
#
# ra.gate.was_on_synced records that ra.gate.was_on is known to match the blocks
# in the world. It is absent on a gate that has never run under this version and
# is cleared periodically by uni_gate/tick, and while absent the output is written
# unconditionally — that is what repairs a hand-edited shell and what migrates
# gates placed before the change-detection existed.
execute if entity @s[tag=!ra.gate.was_on_synced,tag=ra.out_success] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute if entity @s[tag=!ra.gate.was_on_synced,tag=!ra.out_success] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

execute if entity @s[tag=ra.gate.was_on_synced,tag=ra.out_success,tag=!ra.gate.was_on] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute if entity @s[tag=ra.gate.was_on_synced,tag=!ra.out_success,tag=ra.gate.was_on] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

tag @s add ra.gate.was_on_synced
tag @s[tag=ra.out_success] add ra.gate.was_on
tag @s[tag=!ra.out_success] remove ra.gate.was_on
