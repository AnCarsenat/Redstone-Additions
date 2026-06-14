# /ra:tools/wrench/try_assemble_iron
# Try to assemble an iron-tier multiblock
# Context: at multiblock base position

# No iron-tier multiblocks yet
tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No iron-tier multiblocks available yet.",color:"gray"}]
playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.8
