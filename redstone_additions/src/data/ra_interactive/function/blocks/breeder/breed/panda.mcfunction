# /ra_interactive:blocks/breeder/breed/panda
# Try to breed pandas with bamboo. Positioned at breeder, checking in front.
# Note: Pandas need 8 bamboo blocks within 5 blocks to breed normally, but breeder bypasses this

execute positioned ^ ^ ^1 unless entity @e[type=panda,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0
execute positioned ^ ^ ^1 as @e[type=panda,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
data modify storage ra:temp breed_item set value "bamboo"
function ra_interactive:blocks/breeder/breed_success
