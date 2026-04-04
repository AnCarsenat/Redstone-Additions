# /ra:tools/data_handler/run_action
# Execute selected data handler menu action.

execute unless entity @e[tag=ra.dh_target,limit=1] run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"No selected target. Shift+RMB a block first.",color:"red"}]
execute unless entity @e[tag=ra.dh_target,limit=1] run scoreboard players set @s ra.dh.pending 0
execute unless entity @e[tag=ra.dh_target,limit=1] run scoreboard players set @s ra.dh.action 0
execute unless entity @e[tag=ra.dh_target,limit=1] run scoreboard players enable @s ra.dh.action
execute unless entity @e[tag=ra.dh_target,limit=1] run return 0

# Numeric properties
execute if score @s ra.dh.action matches 1 run scoreboard players set @s ra.dh.pending 1
execute if score @s ra.dh.action matches 1 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for range.",color:"gray"}]
execute if score @s ra.dh.action matches 1 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 6 run scoreboard players set @s ra.dh.pending 6
execute if score @s ra.dh.action matches 6 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for delay.",color:"gray"}]
execute if score @s ra.dh.action matches 6 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 7 run scoreboard players set @s ra.dh.pending 7
execute if score @s ra.dh.action matches 7 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for cooldown.",color:"gray"}]
execute if score @s ra.dh.action matches 7 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 8 run scoreboard players set @s ra.dh.pending 8
execute if score @s ra.dh.action matches 8 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for power.",color:"gray"}]
execute if score @s ra.dh.action matches 8 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 14 run scoreboard players set @s ra.dh.pending 14
execute if score @s ra.dh.action matches 14 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for distance.",color:"gray"}]
execute if score @s ra.dh.action matches 14 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 15 run scoreboard players set @s ra.dh.pending 15
execute if score @s ra.dh.action matches 15 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for extend.",color:"gray"}]
execute if score @s ra.dh.action matches 15 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 16 run scoreboard players set @s ra.dh.pending 16
execute if score @s ra.dh.action matches 16 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for pulse.",color:"gray"}]
execute if score @s ra.dh.action matches 16 run function ra:tools/data_handler/request_number

execute if score @s ra.dh.action matches 17 run scoreboard players set @s ra.dh.pending 17
execute if score @s ra.dh.action matches 17 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Enter new number for chance.",color:"gray"}]
execute if score @s ra.dh.action matches 17 run function ra:tools/data_handler/request_number

# Text properties
execute if score @s ra.dh.action matches 2 run scoreboard players set @s ra.dh.pending 2
execute if score @s ra.dh.action matches 2 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for message in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 2 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 3 run scoreboard players set @s ra.dh.pending 3
execute if score @s ra.dh.action matches 3 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for tag in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 3 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 4 run scoreboard players set @s ra.dh.pending 4
execute if score @s ra.dh.action matches 4 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for entity_selector in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 4 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 9 run scoreboard players set @s ra.dh.pending 9
execute if score @s ra.dh.action matches 9 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for mode in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 9 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 10 run scoreboard players set @s ra.dh.pending 10
execute if score @s ra.dh.action matches 10 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for target in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 10 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 12 run scoreboard players set @s ra.dh.pending 12
execute if score @s ra.dh.action matches 12 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for gate_type in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 12 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 13 run scoreboard players set @s ra.dh.pending 13
execute if score @s ra.dh.action matches 13 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for channel in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 13 run function ra:tools/data_handler/request_text

execute if score @s ra.dh.action matches 18 run scoreboard players set @s ra.dh.pending 18
execute if score @s ra.dh.action matches 18 run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Write new text for gate in the input book.",color:"gray"}]
execute if score @s ra.dh.action matches 18 run function ra:tools/data_handler/request_text

# Boolean toggles
execute if score @s ra.dh.action matches 5 if data entity @e[tag=ra.dh_target,limit=1] {data:{properties:{inverted:1b}}} run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.inverted set value 0b
execute if score @s ra.dh.action matches 5 unless data entity @e[tag=ra.dh_target,limit=1] {data:{properties:{inverted:1b}}} run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.inverted set value 1b
execute if score @s ra.dh.action matches 5 run function ra:tools/data_handler/refresh

execute if score @s ra.dh.action matches 11 if data entity @e[tag=ra.dh_target,limit=1] {data:{properties:{enabled:1b}}} run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.enabled set value 0b
execute if score @s ra.dh.action matches 11 unless data entity @e[tag=ra.dh_target,limit=1] {data:{properties:{enabled:1b}}} run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.enabled set value 1b
execute if score @s ra.dh.action matches 11 run function ra:tools/data_handler/refresh

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
