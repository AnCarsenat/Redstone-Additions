# /ra_interactive:blocks/breeder/drop_items
# Drop all items from the breeder container

# Loop through all items and drop them
execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[0]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[1] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[1]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[2] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[2]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[3] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[3]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[4] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[4]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[5] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[5]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[6] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[6]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[7] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[7]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp

execute if data block ~ ~ ~ Items[8] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[8]
execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp
