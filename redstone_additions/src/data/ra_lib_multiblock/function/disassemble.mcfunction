# /ra_lib_multiblock:disassemble
# Disassemble multiblock at current position
# Context: as multiblock marker entity

# Type-specific cleanup via registered hooks
function #ra_lib_multiblock:on_break

# Effects
execute at @s run playsound minecraft:block.anvil.land block @a[distance=..16] ~ ~ ~ 0.5 0.8
execute at @s run particle minecraft:smoke ~ ~0.5 ~ 0.3 0.3 0.3 0.02 15

tellraw @a[distance=..16] [{text:"[RA] ",color:"gold"},{text:"Multiblock disassembled.",color:"yellow"}]

# Remove marker
kill @s
