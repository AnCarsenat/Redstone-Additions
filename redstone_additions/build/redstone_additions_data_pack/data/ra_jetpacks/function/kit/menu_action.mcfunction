# /ra_jetpacks:kit/menu_action
# Handle one /trigger ra.jp.kits value. Context: as the player, at the player.
#
#    1  open the menu          (what a bare /trigger ra.jp.kits does)
#   11..13  toggle a kit off or on
#   21..23  remove a kit and hand the item back
#
# The score is consumed and the trigger re-enabled first, so a command that fails
# partway cannot leave the player stuck with a trigger they can never fire again.

execute store result score #jp.act ra.temp run scoreboard players get @s ra.jp.kits
scoreboard players set @s ra.jp.kits 0
scoreboard players enable @s ra.jp.kits

execute if score #jp.act ra.temp matches 11 run function ra_jetpacks:kit/toggle {mute:"ra.jp.mute_speed",label:"Thruster"}
execute if score #jp.act ra.temp matches 12 run function ra_jetpacks:kit/toggle {mute:"ra.jp.mute_lift",label:"Lift"}
execute if score #jp.act ra.temp matches 13 run function ra_jetpacks:kit/toggle {mute:"ra.jp.mute_scorch",label:"Scorch"}

execute if score #jp.act ra.temp matches 21 run function ra_jetpacks:kit/remove {bit:1,key:"jp_speed",kit:"speed",label:"Thruster"}
execute if score #jp.act ra.temp matches 22 run function ra_jetpacks:kit/remove {bit:2,key:"jp_lift",kit:"lift",label:"Lift"}
execute if score #jp.act ra.temp matches 23 run function ra_jetpacks:kit/remove {bit:4,key:"jp_scorch",kit:"scorch",label:"Scorch"}

function ra_jetpacks:kit/menu
