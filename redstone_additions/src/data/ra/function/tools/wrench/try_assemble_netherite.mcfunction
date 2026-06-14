# /ra:tools/wrench/try_assemble_netherite
# Try to assemble a netherite-tier multiblock (Upgrade Platform)
# Context: at multiblock base position

# Set type for validation
data modify storage ra:multiblock type set value "upgrade_platform"

# Try to assemble
execute store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_assemble

# Feedback
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"Invalid Upgrade Platform structure.",color:"red"}]
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"Need: 3x3 smooth stone platform with netherite base in center",color:"gray"}]
execute if score #mb_assembled ra.temp matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.5
