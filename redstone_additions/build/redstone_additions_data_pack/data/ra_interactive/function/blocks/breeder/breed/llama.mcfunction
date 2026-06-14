# /ra_interactive:blocks/breeder/breed/llama
# Try to breed llamas with hay bale. Positioned at breeder, checking in front.

# Llama
execute positioned ^ ^ ^1 if entity @e[type=llama,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] as @e[type=llama,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] run data modify entity @s InLove set value 600
execute positioned ^ ^ ^1 if entity @e[type=llama,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run data modify storage ra:temp breed_item set value "hay_block"
execute positioned ^ ^ ^1 if entity @e[type=llama,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

# Trader llama
execute positioned ^ ^ ^1 if entity @e[type=trader_llama,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] as @e[type=trader_llama,distance=..2,limit=1,nbt={Age:0,InLove:0,Tame:1b}] run data modify entity @s InLove set value 600
execute positioned ^ ^ ^1 if entity @e[type=trader_llama,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run data modify storage ra:temp breed_item set value "hay_block"
execute positioned ^ ^ ^1 if entity @e[type=trader_llama,distance=..2,limit=1,nbt={Age:0,Tame:1b}] run function ra_interactive:blocks/breeder/breed_success
