# /ra:tools/wrench/cycle_block
# Cycle block type. As custom block, at block position.

# Mark that we found a block
data modify storage ra:temp wrench_found set value 1b

# UNI Gate: Cycle through gate types
execute if entity @s[tag=ra.custom_block.uni_gate] run return run function ra:tools/wrench/cycle_uni_gate

# Other blocks could be added here

# Throughput, three steps, on the blocks whose whole job is a rate.
execute if entity @s[tag=ra.custom_block.liquid_drain] run return run function ra_wires:blocks/liquid_drain/cycle_rate
execute if entity @s[tag=ra.custom_block.electric_consumer] run return run function ra_wires:blocks/electric_consumer/cycle_rate

execute if entity @s[tag=ra.custom_block.ender_item_vault] run return run function ra_ender:blocks/item_vault/cycle_mode
execute if entity @s[tag=ra.custom_block.ender_fluid_vault] run return run function ra_ender:blocks/fluid_vault/cycle_mode
execute if entity @s[tag=ra.custom_block.ender_power_vault] run return run function ra_ender:blocks/power_vault/cycle_mode

# If block doesn't support cycling
tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"This block doesn't support cycling.",color:"gray"}]
