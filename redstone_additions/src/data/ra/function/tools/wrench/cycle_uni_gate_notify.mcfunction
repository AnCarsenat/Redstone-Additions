# /ra:tools/wrench/cycle_uni_gate_notify
# Display notification after cycling. As armor stand, at position.

# Get the new gate type for display
data modify storage ra:temp current_gate set from entity @s data.properties.gate_type

# Notify player
tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"UNI Gate mode: ",color:"gray"},{nbt:"current_gate",storage:"ra:temp",color:"light_purple",bold:true}]

# Play sound
playsound minecraft:block.note_block.hat block @a[distance=..16] ~ ~ ~ 0.5 2

# Visual feedback
particle minecraft:happy_villager ~ ~0.5 ~ 0.3 0.3 0.3 0 5

# Clear storage
data remove storage ra:temp current_gate
