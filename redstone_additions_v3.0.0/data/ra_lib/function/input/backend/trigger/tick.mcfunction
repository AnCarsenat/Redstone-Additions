# /ra_lib:input/backend/trigger/tick
# Detect trigger submissions and validate them.

execute if score @s ra.input.trigger > @s ra.input.last run function ra_lib:input/backend/trigger/submit
execute if score @s ra.input.trigger < @s ra.input.last run function ra_lib:input/backend/trigger/submit