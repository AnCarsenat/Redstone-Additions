# /ra_lib:input/router/open
# Open the selected backend UI for the current mode.

execute if score @s ra.input.backend matches 1 if score @s ra.input.mode matches 1 run function ra_lib:input/backend/trigger/open_choice
execute if score @s ra.input.backend matches 1 if score @s ra.input.mode matches 3 run function ra_lib:input/backend/trigger/open_choice
execute if score @s ra.input.backend matches 2 if score @s ra.input.mode matches 2 run function ra_lib:input/backend/writable_book/open_text