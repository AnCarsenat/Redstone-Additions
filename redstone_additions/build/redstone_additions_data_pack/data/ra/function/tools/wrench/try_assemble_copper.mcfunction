# /ra:tools/wrench/try_assemble_copper
# Try to assemble a copper-tier multiblock (Blast Forge)
# Context: at multiblock base position

# Set type for validation
data modify storage ra:multiblock type set value "blast_forge"

# Try to assemble
execute store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_assemble

# Feedback
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"Invalid Blast Forge structure.",color:"red"}]
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"Need: 3x3x3 nether bricks + blast furnace + 2 barrels (input) + 1 barrel (output)",color:"gray"}]
execute if score #mb_assembled ra.temp matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.5
