# /ra:tools/data_handler/list_properties
# List all properties with clickable value and type fields. As player.

# Check each common property and display if it exists
# We need to check each property type and display accordingly

# Check for 'range' property (typically int)
execute if data storage ra:dh properties.range run function ra:tools/data_handler/props/show_range

# Check for 'message' property (typically str)
execute if data storage ra:dh properties.message run function ra:tools/data_handler/props/show_message

# Check for 'tag' property (typically str)
execute if data storage ra:dh properties.tag run function ra:tools/data_handler/props/show_tag

# Check for 'entity_selector' property (typically str)
execute if data storage ra:dh properties.entity_selector run function ra:tools/data_handler/props/show_entity_selector

# Check for 'inverted' property (typically bool)
execute if data storage ra:dh properties.inverted run function ra:tools/data_handler/props/show_inverted


# Check for 'delay' property (typically int)
execute if data storage ra:dh properties.delay run function ra:tools/data_handler/props/show_delay

# Check for 'cooldown' property (typically int)
execute if data storage ra:dh properties.cooldown run function ra:tools/data_handler/props/show_cooldown

# Check for 'power' property (typically int)
execute if data storage ra:dh properties.power run function ra:tools/data_handler/props/show_power

# Check for 'mode' property (typically str)
execute if data storage ra:dh properties.mode run function ra:tools/data_handler/props/show_mode

# Check for 'target' property (typically str)
execute if data storage ra:dh properties.target run function ra:tools/data_handler/props/show_target

# Check for 'enabled' property (typically bool)
execute if data storage ra:dh properties.enabled run function ra:tools/data_handler/props/show_enabled

# Check for legacy 'gate' property (str: AND, OR, NOT, XOR, NAND, NOR, XNOR)
execute if data storage ra:dh properties.gate run function ra:tools/data_handler/props/show_gate

# Check for 'gate_type' property (str: and, or, not, xor, nand, nor, xnor)
execute if data storage ra:dh properties.gate_type run function ra:tools/data_handler/props/show_gate_type

# Check for 'channel' property (typically int) - for wireless emitter/receiver
execute if data storage ra:dh properties.channel run function ra:tools/data_handler/props/show_channel

# Check for 'distance' property (int) - for beamer
execute if data storage ra:dh properties.distance run function ra:tools/data_handler/props/show_distance

# Check for 'extend' property (int) - for extender
execute if data storage ra:dh properties.extend run function ra:tools/data_handler/props/show_extend

# Check for 'pulse' property (int) - for shortener
execute if data storage ra:dh properties.pulse run function ra:tools/data_handler/props/show_pulse

# Check for 'chance' property (int) - for randomizer (percentage 0-100)
execute if data storage ra:dh properties.chance run function ra:tools/data_handler/props/show_chance

# If no properties, show message
execute unless data storage ra:dh properties run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]
