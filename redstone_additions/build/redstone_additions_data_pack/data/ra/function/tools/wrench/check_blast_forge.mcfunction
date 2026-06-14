# /ra:tools/wrench/check_blast_forge
# Check if looking at a valid blast forge structure
# Context: at hopper position

# Mark that we found something (hopper)
data modify storage ra:temp wrench_found set value 1b

# Check if already a multiblock (use aligned position + tight distance)
execute align xyz positioned ~0.5 ~ ~0.5 if entity @e[tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest] run return run function ra:tools/wrench/toggle_multiblock

# Set type for validation
data modify storage ra:multiblock type set value "blast_forge"

# Store player rotation for directional multiblock
execute store result storage ra:multiblock rotation int 1 positioned as @a[tag=ra.wrench_active,distance=..8,limit=1,sort=nearest] run data get entity @s Rotation[0]

# Try to assemble - show error if structure is invalid
execute store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_assemble
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"Invalid Blast Forge structure. Need: 3x3 brick wall + blast furnace behind.",color:"red"}]
