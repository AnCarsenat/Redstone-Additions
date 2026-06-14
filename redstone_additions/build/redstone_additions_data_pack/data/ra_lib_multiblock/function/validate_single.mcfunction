# /ra_lib_multiblock:validate_single
# Validate a single multiblock structure
# Context: as multiblock marker, at its position

# Grace period — skip validation for newly created markers, remove tag for next time
execute if entity @s[tag=ra.multiblock.grace] run return run tag @s remove ra.multiblock.grace

# Reset validation flag
scoreboard players set @s ra.multiblock 0

# Type-specific structure check via registered hooks (sets score to 1 if valid)
function #ra_lib_multiblock:check_structure

# Debug: show validation result
execute if entity @a[tag=ra.debug,limit=1] if score @s ra.multiblock matches 0 at @s run tellraw @a[tag=ra.debug] [{text:"[MB Debug] ",color:"gold"},{text:"FAIL ",color:"red"},{text:"type="},{nbt:"data.type",entity:"@s",color:"aqua"},{text:" facing="},{nbt:"data.facing",entity:"@s",color:"aqua"},{text:" pos="},{nbt:"Pos",entity:"@s",color:"yellow"}]
execute if entity @a[tag=ra.debug,limit=1] if score @s ra.multiblock matches 1 at @s run tellraw @a[tag=ra.debug] [{text:"[MB Debug] ",color:"gold"},{text:"OK ",color:"green"},{text:"type="},{nbt:"data.type",entity:"@s",color:"aqua"},{text:" facing="},{nbt:"data.facing",entity:"@s",color:"aqua"}]

# If structure is broken, disassemble
execute if score @s ra.multiblock matches 0 run function ra_lib_multiblock:disassemble
