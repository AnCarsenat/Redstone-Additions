# /ra_interactive:blocks/breeder/breed/chicken_wheat_seeds
# Try to breed chickens with wheat seeds. Positioned at breeder, checking in front.

execute positioned ^ ^ ^1 unless entity @e[type=chicken,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0
execute positioned ^ ^ ^1 as @e[type=chicken,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
data modify storage ra:temp breed_item set value "wheat_seeds"
function ra_interactive:blocks/breeder/breed_success
