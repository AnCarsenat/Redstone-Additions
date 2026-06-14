# /ra_interactive:blocks/breeder/breed/cow
# Try to breed cows with wheat. Positioned at breeder, checking in front.

# Check for adult cow that can breed (Age:0 = adult, InLove:0 = not in love mode)
execute positioned ^ ^ ^1 unless entity @e[type=cow,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0

# Set the cow into love mode (InLove ticks)
execute positioned ^ ^ ^1 as @e[type=cow,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600

# Consume food and show effects
data modify storage ra:temp breed_item set value "wheat"
function ra_interactive:blocks/breeder/breed_success
