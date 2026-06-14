# /ra_lib:input/open_text
# Open a free-text request for the executing player.
# Defaults: max chars 120, backend auto-selected by mode.

scoreboard players set @s ra.input.mode 2
scoreboard players set @s ra.input.min 0
scoreboard players set @s ra.input.max 120

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open
