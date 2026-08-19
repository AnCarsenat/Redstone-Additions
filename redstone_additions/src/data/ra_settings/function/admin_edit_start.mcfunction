# /ra_settings:admin_edit_start
# Ask an operator to type a value for the global setting named in
# storage ra:settings admin_edit. Context: as the operator.
#
# The generated admin functions cannot carry a typed value: a function path
# autocompletes, but it is fixed text, so /function .../edit is as far as the
# command tree can take an arbitrary number or string. From here it is the same
# ra_lib:input form the Data Handler uses, so operators and players type into the
# same thing.

function ra_lib:input/cancel

scoreboard players set @s ra.settings.pend -1

execute if data storage ra:settings admin_edit{type:"str"} run scoreboard players set @s ra.input.mode 2
execute if data storage ra:settings admin_edit{type:"str"} run scoreboard players set @s ra.input.min 0
execute if data storage ra:settings admin_edit{type:"str"} run scoreboard players set @s ra.input.max 120

execute if data storage ra:settings admin_edit{type:"int"} run scoreboard players set @s ra.input.mode 3
execute if data storage ra:settings admin_edit{type:"int"} store result score @s ra.input.min run data get storage ra:settings admin_edit.min
execute if data storage ra:settings admin_edit{type:"int"} store result score @s ra.input.max run data get storage ra:settings admin_edit.max

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open
