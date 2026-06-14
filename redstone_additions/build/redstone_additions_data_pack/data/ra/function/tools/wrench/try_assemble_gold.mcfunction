# /ra:tools/wrench/try_assemble_gold
# Try to assemble a gold-tier multiblock
# Context: at multiblock base position

# No gold-tier multiblocks yet
tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No gold-tier multiblocks available yet.",color:"gray"}]
playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.8
