# /ra:tools/data_handler/request_number
# Open a numeric input request for the pending property edit.

function ra_lib:input/cancel

scoreboard players set @s ra.input.mode 3
scoreboard players set @s ra.input.min 0
scoreboard players set @s ra.input.max 9999

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open
