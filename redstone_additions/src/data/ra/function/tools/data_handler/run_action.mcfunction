# /ra:tools/data_handler/run_action
# Execute selected data handler menu action.

execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"No selected target. Shift+RMB a block first.",color:"red"}]
execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run scoreboard players set @s ra.dh.pending 0
execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run scoreboard players set @s ra.dh.action 0
execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run scoreboard players enable @s ra.dh.action
execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run return 0

# Property rows carry 100 + their registry index, so one handler serves every
# property instead of a branch per name. Everything below 100 is a menu action.
execute if score @s ra.dh.action matches 200.. run function ra:tools/data_handler/props/hand_action
execute if score @s ra.dh.action matches 100..199 run function ra:tools/data_handler/generic_action

# View and refresh actions
execute if score @s ra.dh.action matches 90 run function ra:tools/data_handler/show_internal_data
execute if score @s ra.dh.action matches 91 run function ra:tools/data_handler/refresh
execute if score @s ra.dh.action matches 92 run function ra:tools/data_handler/show_menu

# Cancel currently pending edit/input
execute if score @s ra.dh.action matches 93 run function ra_lib:input/cancel
execute if score @s ra.dh.action matches 93 run scoreboard players set @s ra.dh.pending 0
execute if score @s ra.dh.action matches 93 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Pending edit canceled.",color:"gray"}]

scoreboard players set @s ra.dh.action 0
scoreboard players enable @s ra.dh.action
