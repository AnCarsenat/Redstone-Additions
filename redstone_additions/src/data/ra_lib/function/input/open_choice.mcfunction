# /ra_lib:input/open_choice
# Open a numeric choice request for the executing player.
# Defaults: range 0..9, backend auto-selected by mode.

scoreboard players set @s ra.input.mode 1
scoreboard players set @s ra.input.min 0
scoreboard players set @s ra.input.max 9

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open
