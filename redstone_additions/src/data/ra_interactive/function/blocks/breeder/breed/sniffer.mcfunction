# /ra_interactive:blocks/breeder/breed/sniffer
# Try to breed sniffers with torchflower seeds. Positioned at breeder, checking in front.

execute positioned ^ ^ ^1 unless entity @e[type=sniffer,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0
execute positioned ^ ^ ^1 as @e[type=sniffer,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
data modify storage ra:temp breed_item set value "torchflower_seeds"
function ra_interactive:blocks/breeder/breed_success
