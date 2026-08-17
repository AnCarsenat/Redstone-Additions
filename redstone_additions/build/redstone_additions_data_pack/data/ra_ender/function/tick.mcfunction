# /ra_ender:tick
# Tick every ender block.

# Each block's tick opens with several @e scans over its own markers, so a world
# with none of them still paid for the scans. One existence check each is cheaper
# than the four or five selectors behind it.
execute if entity @e[type=marker,tag=ra.custom_block.ender_item_vault,limit=1] run function ra_ender:blocks/item_vault/tick
execute if entity @e[type=marker,tag=ra.custom_block.ender_fluid_vault,limit=1] run function ra_ender:blocks/fluid_vault/tick
execute if entity @e[type=marker,tag=ra.custom_block.ender_power_vault,limit=1] run function ra_ender:blocks/power_vault/tick
execute if entity @e[type=marker,tag=ra.custom_block.teleport_anchor,limit=1] run function ra_ender:blocks/teleport_anchor/tick

# Arrival grace, so a player landing on an anchor that is itself powered is not
# bounced straight back.
# Same reason as the anchor cooldown: a player with no grace score at all would
# never match the selector that looks for one at or below zero.
scoreboard players add @a ra.ender.grace 0
execute if entity @a[scores={ra.ender.grace=1..}] run scoreboard players remove @a[scores={ra.ender.grace=1..}] ra.ender.grace 1
