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

# Say that the request went out. ra_lib:input prints its own instructions once a
# backend opens, so if this line appears and nothing follows it, the failure is in
# the backend rather than in the settings code that asked for it.
function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open

# Reported AFTER the backend is chosen, not before, so the numbers are the ones
# that were actually used. mode 2 is text and should pick backend 2 (the writable
# book); mode 3 is a number and should pick backend 1. A backend of 0 means
# nothing opened at all, and no amount of waiting will produce an answer.
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Waiting for your input",color:"gray"},{text:"  (mode ",color:"dark_gray"},{score:{name:"@s",objective:"ra.input.mode"},color:"dark_gray"},{text:", backend ",color:"dark_gray"},{score:{name:"@s",objective:"ra.input.backend"},color:"dark_gray"},{text:")",color:"dark_gray"}]
execute if score @s ra.input.backend matches 0 run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"No input method opened — nothing will arrive. Report the mode/backend above.",color:"red"}]
