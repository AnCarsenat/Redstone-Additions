# /ra_multiblock:drills/rock_metallic/tick
# Per-tick processing for the Rock Metallic Drill.
# Context: as the multiblock marker, at the base position.
#
# There is no per-facing copy of this logic. The IO helpers resolve "output_1"
# and "redstone_in" from the marker's own IO maps, which the library already
# rotated to match the assembled facing.

# Skip if switched off via properties.
execute unless data entity @s data.properties{enabled:1b} run return 0

# A redstone block on the control side halts the drill.
execute store result score #mb_ctrl ra.multiblock run function ra_lib_multiblock:io/is_block {name:"redstone_in",block:"minecraft:redstone_block"}
execute if score #mb_ctrl ra.multiblock matches 1 run return 0

# One rock every 40 ticks. The original produced one every single tick.
scoreboard players add @s ra.cooldown 1
execute unless score @s ra.cooldown matches 40.. run return 0
scoreboard players set @s ra.cooldown 0

particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force
playsound minecraft:block.stone.break block @a[distance=..12] ~ ~ ~ 0.4 0.7

# Build the item first, then let the library position the insert. Passing an
# item's components through macro arguments would mean quoting them twice.
data modify storage ra:multiblock io_item set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"dead_brain_coral_block","minecraft:rarity":"common"}}
function ra_lib_multiblock:io/at {name:"output_1",run:"ra_lib_multiblock:io/insert_here"}
