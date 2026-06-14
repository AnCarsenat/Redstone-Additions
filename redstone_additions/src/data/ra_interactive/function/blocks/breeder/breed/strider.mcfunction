# /ra_interactive:blocks/breeder/breed/strider
# Try to breed striders with warped fungus. Positioned at breeder, checking in front.

execute positioned ^ ^ ^1 unless entity @e[type=strider,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0
execute positioned ^ ^ ^1 as @e[type=strider,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
data modify storage ra:temp breed_item set value "warped_fungus"
function ra_interactive:blocks/breeder/breed_success
