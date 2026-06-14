# /ra_lib:input/open_number
# Open a numeric input request for the executing player.
# Defaults: range 1..9, backend auto-selected by mode.

scoreboard players set @s ra.input.mode 3
scoreboard players set @s ra.input.min 1
scoreboard players set @s ra.input.max 9

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open
