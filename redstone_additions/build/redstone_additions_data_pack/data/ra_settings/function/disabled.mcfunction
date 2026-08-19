# /ra_settings:disabled
# Every block currently switched off, and a button to switch each back on.
# Context: as a player.
#
# The disabled list is stored as what is OFF, which keeps the common case free --
# but it also means a disabled block leaves no trace anywhere a player looks. The
# block simply refuses to place. This is the page that answers "why can I not
# build this", and it is why ra:load says something when the list is not empty.

tellraw @s [{text:""},{text:"─── ",color:"dark_gray"},{text:"Disabled Blocks",color:"red",bold:true},{text:" ───",color:"dark_gray"}]

execute unless data storage ra:settings disabled[0] run tellraw @s [{text:"  Nothing is disabled — every block can be placed.",color:"green"}]
execute unless data storage ra:settings disabled[0] run return 0

data modify storage ra:settings dscan set from storage ra:settings disabled
function ra_settings:disabled_step
data remove storage ra:settings dscan

tellraw @s [{text:"  Disabled blocks cannot be PLACED. Ones already built keep working,",color:"dark_gray"}]
tellraw @s [{text:"  and the item can still be crafted and held.",color:"dark_gray"}]
