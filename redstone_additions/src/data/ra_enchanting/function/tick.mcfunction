# /ra_enchanting:tick
# Look for sacrificed items lying on an enchanting table.
#
# This is the one global item selector in the pack, so it runs once every five
# ticks instead of every tick. A sacrificed stack rolls once per second, which
# is four scan steps — see sacrifice/on_table.

scoreboard players add #scan ra.ench.scan 1
execute unless score #scan ra.ench.scan matches 5.. run return 0
scoreboard players set #scan ra.ench.scan 0

# Items already produced by a sacrifice carry ra.ench.done and are skipped, so a
# result never immediately feeds itself back into the table it landed on.
tag @e[type=item,tag=ra.ench.on_table] remove ra.ench.on_table

# An item resting on a table sits inside the table's own block space; one that
# is still bouncing sits in the space above it.
execute as @e[type=item,tag=!ra.ench.done] at @s if block ~ ~ ~ minecraft:enchanting_table run tag @s add ra.ench.on_table
execute as @e[type=item,tag=!ra.ench.done,tag=!ra.ench.on_table] at @s if block ~ ~-1 ~ minecraft:enchanting_table run tag @s add ra.ench.on_table

execute as @e[type=item,tag=ra.ench.on_table] at @s run function ra_enchanting:sacrifice/on_table
