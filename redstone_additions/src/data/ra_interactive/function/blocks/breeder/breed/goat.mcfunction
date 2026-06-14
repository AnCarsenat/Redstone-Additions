# /ra_interactive:blocks/breeder/breed/goat
# Try to breed goats with wheat. Positioned at breeder, checking in front.

execute positioned ^ ^ ^1 unless entity @e[type=goat,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0
execute positioned ^ ^ ^1 as @e[type=goat,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
data modify storage ra:temp breed_item set value "wheat"
function ra_interactive:blocks/breeder/breed_success
