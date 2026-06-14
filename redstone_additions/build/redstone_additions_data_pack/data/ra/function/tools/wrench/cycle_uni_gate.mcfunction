# /ra:tools/wrench/cycle_uni_gate
# Cycle UNI gate through different gate modes. As armor stand, at position.

# Initialize gate_type if not set
execute unless data entity @s data.properties.gate_type run data modify entity @s data.properties.gate_type set value "and"

# Cycle to next gate type based on current value (use return to prevent multiple changes)
execute if data entity @s data.properties{gate_type:"xnor"} run data modify entity @s data.properties.gate_type set value "and"
execute if data entity @s data.properties{gate_type:"xnor"} run return run function ra:tools/wrench/cycle_uni_gate_notify

execute if data entity @s data.properties{gate_type:"nor"} run data modify entity @s data.properties.gate_type set value "xnor"
execute if data entity @s data.properties{gate_type:"nor"} run return run function ra:tools/wrench/cycle_uni_gate_notify

execute if data entity @s data.properties{gate_type:"nand"} run data modify entity @s data.properties.gate_type set value "nor"
execute if data entity @s data.properties{gate_type:"nand"} run return run function ra:tools/wrench/cycle_uni_gate_notify

execute if data entity @s data.properties{gate_type:"xor"} run data modify entity @s data.properties.gate_type set value "nand"
execute if data entity @s data.properties{gate_type:"xor"} run return run function ra:tools/wrench/cycle_uni_gate_notify

execute if data entity @s data.properties{gate_type:"not"} run data modify entity @s data.properties.gate_type set value "xor"
execute if data entity @s data.properties{gate_type:"not"} run return run function ra:tools/wrench/cycle_uni_gate_notify

execute if data entity @s data.properties{gate_type:"or"} run data modify entity @s data.properties.gate_type set value "not"
execute if data entity @s data.properties{gate_type:"or"} run return run function ra:tools/wrench/cycle_uni_gate_notify

execute if data entity @s data.properties{gate_type:"and"} run data modify entity @s data.properties.gate_type set value "or"
function ra:tools/wrench/cycle_uni_gate_notify
