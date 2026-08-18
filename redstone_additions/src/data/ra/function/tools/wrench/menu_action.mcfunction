# /ra:tools/wrench/menu_action
# A [Cycle] button was clicked. Context: as the player, at the player.
#
# The trigger value is the row index. The block is whichever one this player last
# opened a menu on, remembered as three scores -- see open_menu for why it is not
# a re-raycast.

execute store result score #wr.act ra.temp run scoreboard players get @s ra.wrench
scoreboard players remove #wr.act ra.temp 1
scoreboard players set @s ra.wrench 0
scoreboard players enable @s ra.wrench

execute unless score @s ra.wr.x matches -2147483648.. run return run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"Nothing selected — shift-click a block first.",color:"gray"}]

execute store result storage ra:wrench at.x int 1 run scoreboard players get @s ra.wr.x
execute store result storage ra:wrench at.y int 1 run scoreboard players get @s ra.wr.y
execute store result storage ra:wrench at.z int 1 run scoreboard players get @s ra.wr.z
execute store result storage ra:wrench at.i int 1 run scoreboard players get #wr.act ra.temp

tag @s add ra.wrench_user
function ra:tools/wrench/menu_at with storage ra:wrench at
tag @s remove ra.wrench_user
