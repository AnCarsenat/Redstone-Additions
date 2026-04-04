# /ra:tools/data_handler/found_block
# Called when a custom block is found. As armor stand, at armor stand.

# Mark that we found a block (prevent multiple calls)
data modify storage ra:temp dh_found set value 1b

# Tag this block so we can reference it
tag @s add ra.dh_target

# Store block UUID for reference
data modify storage ra:dh target set from entity @s UUID

# Get block name from tag - check each known block type
data modify storage ra:dh block_type set value "Unknown Block"

# Interactive blocks
execute if entity @s[tag=ra.custom_block.message_block] run data modify storage ra:dh block_type set value "Message Block"
execute if entity @s[tag=ra.custom_block.block_placer] run data modify storage ra:dh block_type set value "Block Placer"
execute if entity @s[tag=ra.custom_block.block_breaker] run data modify storage ra:dh block_type set value "Block Breaker"
execute if entity @s[tag=ra.custom_block.breeder] run data modify storage ra:dh block_type set value "Breeder"
execute if entity @s[tag=ra.custom_block.pusher] run data modify storage ra:dh block_type set value "Pusher"
execute if entity @s[tag=ra.custom_block.spitter] run data modify storage ra:dh block_type set value "Spitter"
execute if entity @s[tag=ra.custom_block.item_pipe] run data modify storage ra:dh block_type set value "Item Pipe"
execute if entity @s[tag=ra.custom_block.infinite_water_cauldron] run data modify storage ra:dh block_type set value "Infinite Water Cauldron"
execute if entity @s[tag=ra.custom_block.infinite_lava_cauldron] run data modify storage ra:dh block_type set value "Infinite Lava Cauldron"
execute if entity @s[tag=ra.custom_block.infinite_snow_cauldron] run data modify storage ra:dh block_type set value "Infinite Snow Cauldron"

# Sensor blocks
execute if entity @s[tag=ra.custom_block.entity_detector] run data modify storage ra:dh block_type set value "Entity Detector"
execute if entity @s[tag=ra.custom_block.tag_adder] run data modify storage ra:dh block_type set value "Tag Adder"
execute if entity @s[tag=ra.custom_block.tag_remover] run data modify storage ra:dh block_type set value "Tag Remover"

# Gate blocks
execute if entity @s[tag=ra.custom_block.clock] run data modify storage ra:dh block_type set value "Clock"
execute if entity @s[tag=ra.custom_block.uni_gate] run data modify storage ra:dh block_type set value "UNI Gate"
execute if entity @s[tag=ra.custom_block.delayer] run data modify storage ra:dh block_type set value "Delayer"
execute if entity @s[tag=ra.custom_block.extender] run data modify storage ra:dh block_type set value "Extender"
execute if entity @s[tag=ra.custom_block.rand] run data modify storage ra:dh block_type set value "Randomizer"
execute if entity @s[tag=ra.custom_block.shortener] run data modify storage ra:dh block_type set value "Shortener"

# Clock compatibility: older markers may still use delay instead of cooldown
execute if entity @s[tag=ra.custom_block.clock] if data entity @s data.properties.delay unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set from entity @s data.properties.delay
execute if entity @s[tag=ra.custom_block.clock] if data entity @s data.properties.delay unless data entity @s data.properties.cooldown run data remove entity @s data.properties.delay

# Wireless blocks
execute if entity @s[tag=ra.custom_block.emitter] run data modify storage ra:dh block_type set value "Wireless Emitter"
execute if entity @s[tag=ra.custom_block.receiver] run data modify storage ra:dh block_type set value "Wireless Receiver"

# Store properties and internal data
data modify storage ra:dh properties set from entity @s data.properties
data modify storage ra:dh internal_data set from entity @s data.data
