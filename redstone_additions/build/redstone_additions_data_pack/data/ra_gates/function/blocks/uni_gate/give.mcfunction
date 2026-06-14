# /ra_gates:blocks/uni_gate/give
# Give UNI Gate block item to player
# Universal logic gate - can be any gate type via gate_type property
# Use Creative Data Handler (Shift+RMB) to change gate type: and, or, not, xor, nand, nor, xnor

give @s bat_spawn_egg[item_model="minecraft:smooth_stone_slab",item_name="UNI Gate",custom_data={ra:{uni_gate:1b}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.uni_gate"],Silent:1b,NoAI:1b,Invulnerable:1b}]
