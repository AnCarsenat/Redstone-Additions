# /ra:tools/wrench/try_assemble_copper
# Try to assemble a copper-tier multiblock
# Context: at multiblock base position

# Blast Forge keeps its own hand-written validator, so it is tried by name.
data modify storage ra:multiblock type set value "blast_forge"
execute store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_assemble

# Then anything registered for this tier. Adding a copper-tier structure means
# registering it in ra_multiblock:register_types — this file does not change.
execute if score #mb_assembled ra.temp matches 0 store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_tier {tier:"copper"}

# Feedback
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No copper-tier multiblock matches the structure here.",color:"red"}]
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"Blast Forge: 3x3x3 nether bricks + blast furnace + 2 barrels (input) + 1 barrel (output)",color:"gray"}]
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"Rock Metallic Drill: barrel beside the base, iron bars under the barrel, smooth stone under the base",color:"gray"}]
execute if score #mb_assembled ra.temp matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.5
