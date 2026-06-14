# /ra_interactive:blocks/breeder/breed/bee
# Try to breed bees with flowers. Positioned at breeder, checking in front.

# Check for adult bee not in love mode
execute positioned ^ ^ ^1 unless entity @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0

# Check if we have any flower and consume it (also sets bee into love mode)
function ra_interactive:blocks/breeder/breed/bee_consume_flower
