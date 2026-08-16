# /ra_lib:redstone/count_inputs
# Count how many sides carry a redstone source component, powered or not.
# As entity, at position. Output: @s ra.rs_inputs (0-6).
#
# Mirrors ra_lib:redstone/detect exactly, including the ra.redstone.ignore_blocks
# opt-out: a component only counts as an input on a side if detect would be able
# to read power from it there. Callers that need to tell "input present but off"
# apart from "no input on that side" compare this against the powered count.
# The two must stay in step -- if detect learns a new source, add it here too.

scoreboard players set @s ra.rs_inputs 0

# north: block at ~ ~ ~-1
scoreboard players set #rs_side ra.temp 0
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:lever run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 #minecraft:buttons run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:repeater[facing=south] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:comparator[facing=south] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=south] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=east] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=west] run scoreboard players set #rs_side ra.temp 1
execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~ ~ ~-1 minecraft:redstone_block run scoreboard players set #rs_side ra.temp 1
scoreboard players operation @s ra.rs_inputs += #rs_side ra.temp

# south: block at ~ ~ ~1
scoreboard players set #rs_side ra.temp 0
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:lever run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 #minecraft:buttons run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:repeater[facing=north] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:comparator[facing=north] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=north] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=east] run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=west] run scoreboard players set #rs_side ra.temp 1
execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~ ~ ~1 minecraft:redstone_block run scoreboard players set #rs_side ra.temp 1
scoreboard players operation @s ra.rs_inputs += #rs_side ra.temp

# west: block at ~-1 ~ ~
scoreboard players set #rs_side ra.temp 0
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side] run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up] run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:lever run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ #minecraft:buttons run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:repeater[facing=east] run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:comparator[facing=east] run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=north] run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=south] run scoreboard players set #rs_side ra.temp 1
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=east] run scoreboard players set #rs_side ra.temp 1
execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~-1 ~ ~ minecraft:redstone_block run scoreboard players set #rs_side ra.temp 1
scoreboard players operation @s ra.rs_inputs += #rs_side ra.temp

# east: block at ~1 ~ ~
scoreboard players set #rs_side ra.temp 0
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side] run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up] run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:lever run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ #minecraft:buttons run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:repeater[facing=west] run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:comparator[facing=west] run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=north] run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=south] run scoreboard players set #rs_side ra.temp 1
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=west] run scoreboard players set #rs_side ra.temp 1
execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~1 ~ ~ minecraft:redstone_block run scoreboard players set #rs_side ra.temp 1
scoreboard players operation @s ra.rs_inputs += #rs_side ra.temp

# up: block at ~ ~1 ~
scoreboard players set #rs_side ra.temp 0
execute if block ~ ~1 ~ minecraft:lever run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~1 ~ #minecraft:buttons run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~1 ~ minecraft:redstone_torch run scoreboard players set #rs_side ra.temp 1
execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~ ~1 ~ minecraft:redstone_block run scoreboard players set #rs_side ra.temp 1
scoreboard players operation @s ra.rs_inputs += #rs_side ra.temp

# down: block at ~ ~-1 ~
scoreboard players set #rs_side ra.temp 0
execute if block ~ ~-1 ~ minecraft:lever run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~-1 ~ #minecraft:buttons run scoreboard players set #rs_side ra.temp 1
execute if block ~ ~-1 ~ minecraft:redstone_torch run scoreboard players set #rs_side ra.temp 1
execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~ ~-1 ~ minecraft:redstone_block run scoreboard players set #rs_side ra.temp 1
scoreboard players operation @s ra.rs_inputs += #rs_side ra.temp
