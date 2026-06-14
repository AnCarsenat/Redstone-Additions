# /ra_lib:input/router/tick
# Tick selected backend for the current player session.

execute if score @s ra.input.backend matches 1 run function ra_lib:input/backend/trigger/tick
execute if score @s ra.input.backend matches 2 run function ra_lib:input/backend/writable_book/tick_scan