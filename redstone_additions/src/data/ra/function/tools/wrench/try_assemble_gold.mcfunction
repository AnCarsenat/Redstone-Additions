# /ra:tools/wrench/try_assemble_gold
# Try to assemble a gold-tier multiblock
# Context: at multiblock base position

# No hand-written gold-tier multiblock exists; ask the registry.
execute store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_tier {tier:"gold"}

execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No gold-tier multiblock matches the structure here.",color:"gray"}]
execute if score #mb_assembled ra.temp matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.8
