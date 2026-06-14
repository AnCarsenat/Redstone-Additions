# /ra:tools/wrench/try_assemble_diamond
# Try to assemble a diamond-tier multiblock
# Context: at multiblock base position

# No diamond-tier multiblocks yet
tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No diamond-tier multiblocks available yet.",color:"gray"}]
playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.8
