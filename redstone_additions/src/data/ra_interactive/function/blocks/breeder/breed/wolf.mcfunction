# /ra_interactive:blocks/breeder/breed/wolf
# Try to breed wolves with meat. Positioned at breeder, checking in front.
# Wolves accept any raw or cooked meat (must be tamed)

# Check for tamed adult wolf not in love mode
execute positioned ^ ^ ^1 unless entity @e[type=wolf,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0

# Set wolf into love mode
execute positioned ^ ^ ^1 as @e[type=wolf,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600

# Check each meat type and consume the first one found
execute if items block ~ ~ ~ container.* beef run data modify storage ra:temp breed_item set value "beef"
execute if items block ~ ~ ~ container.* beef run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* cooked_beef run data modify storage ra:temp breed_item set value "cooked_beef"
execute if items block ~ ~ ~ container.* cooked_beef run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* porkchop run data modify storage ra:temp breed_item set value "porkchop"
execute if items block ~ ~ ~ container.* porkchop run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* cooked_porkchop run data modify storage ra:temp breed_item set value "cooked_porkchop"
execute if items block ~ ~ ~ container.* cooked_porkchop run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* chicken run data modify storage ra:temp breed_item set value "chicken"
execute if items block ~ ~ ~ container.* chicken run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* cooked_chicken run data modify storage ra:temp breed_item set value "cooked_chicken"
execute if items block ~ ~ ~ container.* cooked_chicken run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* mutton run data modify storage ra:temp breed_item set value "mutton"
execute if items block ~ ~ ~ container.* mutton run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* cooked_mutton run data modify storage ra:temp breed_item set value "cooked_mutton"
execute if items block ~ ~ ~ container.* cooked_mutton run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* rabbit run data modify storage ra:temp breed_item set value "rabbit"
execute if items block ~ ~ ~ container.* rabbit run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* cooked_rabbit run data modify storage ra:temp breed_item set value "cooked_rabbit"
execute if items block ~ ~ ~ container.* cooked_rabbit run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* rotten_flesh run data modify storage ra:temp breed_item set value "rotten_flesh"
execute if items block ~ ~ ~ container.* rotten_flesh run function ra_interactive:blocks/breeder/breed_success
