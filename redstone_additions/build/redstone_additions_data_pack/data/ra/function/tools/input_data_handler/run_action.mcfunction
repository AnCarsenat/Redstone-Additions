# /ra:tools/input_data_handler/run_action
# Execute selected menu action and re-enable trigger objective.

execute if score @s ra.input_handler.action matches 1 run function ra:tools/input_data_handler/test_number
execute if score @s ra.input_handler.action matches 2 run function ra:tools/input_data_handler/test_text
execute if score @s ra.input_handler.action matches 3 run function ra:tools/input_data_handler/poll
execute if score @s ra.input_handler.action matches 4 run function ra:tools/input_data_handler/consume
execute if score @s ra.input_handler.action matches 5 run function ra_lib:input/cancel
execute if score @s ra.input_handler.action matches 6 run function ra:tools/input_data_handler/show_menu

scoreboard players set @s ra.input_handler.action 0
scoreboard players enable @s ra.input_handler.action