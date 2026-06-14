# /ra:tools/goggles/status/read_status
# Read block properties and display status info via billboard
# Context: as custom block armor stand, at block position

# Read properties from the block's marker entity
data modify storage ra:temp goggles_props set from entity @s data.properties

# Gate blocks — show gate type
execute if entity @s[tag=ra.custom_block.uni_gate] run function ra:tools/goggles/status/read_status_macro

# Wireless blocks — show channel
execute if entity @s[tag=ra.custom_block.emitter] run function ra:tools/goggles/status/read_status_macro
execute if entity @s[tag=ra.custom_block.receiver] run function ra:tools/goggles/status/read_status_macro

# Sensor blocks — show detection info
execute if entity @s[tag=ra.custom_block.entity_detector] run function ra:tools/goggles/status/read_status_macro

# Clock — show cooldown
execute if entity @s[tag=ra.custom_block.clock] run function ra:tools/goggles/status/read_status_macro

# Delayer — show delay
execute if entity @s[tag=ra.custom_block.delayer] run function ra:tools/goggles/status/read_status_macro

# RA Wires blocks — liquid, gas, and electric status
execute if entity @s[tag=ra.wires.node] run function ra_wires:goggles/show_status
