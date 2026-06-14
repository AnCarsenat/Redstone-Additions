# /ra_interactive:blocks/breeder/breed/horse_enchanted_golden_apple
# Try to breed horses/donkeys with enchanted golden apple. Positioned at breeder, checking in front.

# Horse
execute positioned ^ ^ ^1 if entity @e[type=horse,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] as @e[type=horse,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] run data modify entity @s InLove set value 600
execute positioned ^ ^ ^1 if entity @e[type=horse,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run data modify storage ra:temp breed_item set value "enchanted_golden_apple"
execute positioned ^ ^ ^1 if entity @e[type=horse,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

# Donkey
execute positioned ^ ^ ^1 if entity @e[type=donkey,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] as @e[type=donkey,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] run data modify entity @s InLove set value 600
execute positioned ^ ^ ^1 if entity @e[type=donkey,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run data modify storage ra:temp breed_item set value "enchanted_golden_apple"
execute positioned ^ ^ ^1 if entity @e[type=donkey,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run function ra_interactive:blocks/breeder/breed_success
