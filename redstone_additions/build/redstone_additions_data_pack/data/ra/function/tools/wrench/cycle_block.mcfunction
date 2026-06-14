# /ra:tools/wrench/cycle_block
# Cycle block type. As custom block, at block position.

# Mark that we found a block
data modify storage ra:temp wrench_found set value 1b

# UNI Gate: Cycle through gate types
execute if entity @s[tag=ra.custom_block.uni_gate] run return run function ra:tools/wrench/cycle_uni_gate

# Other blocks could be added here

# If block doesn't support cycling
tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"This block doesn't support cycling.",color:"gray"}]
