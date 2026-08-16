# /ra:tools/wrench/try_assemble_netherite
# Try to assemble a netherite-tier multiblock
# Context: at multiblock base position

# Upgrade Platform keeps its own hand-written validator, so it is tried by name.
data modify storage ra:multiblock type set value "upgrade_platform"
execute store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_assemble

# Then anything registered for this tier.
execute if score #mb_assembled ra.temp matches 0 store result score #mb_assembled ra.temp run function ra_lib_multiblock:try_tier {tier:"netherite"}

# Feedback
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No netherite-tier multiblock matches the structure here.",color:"red"}]
execute if score #mb_assembled ra.temp matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"Upgrade Platform: 3x3 smooth stone platform with netherite base in center",color:"gray"}]
execute if score #mb_assembled ra.temp matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.5
