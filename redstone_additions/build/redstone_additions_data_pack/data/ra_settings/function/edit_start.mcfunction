# /ra_settings:edit_start {p,r}
# Ask the player to type a value for row r of page p. Context: as the player.
#
# The same flow the Data Handler uses for a clock's delay: hand over to
# ra_lib:input, which picks a backend the player can actually use, and come back
# for the answer on a later tick. Reusing it rather than inventing a second input
# path means a player types a number into the same form everywhere in the pack.
#
# The row is remembered as a score rather than in storage, because two players
# can be editing different settings at once and storage has one slot.

function ra_lib:input/cancel

# Row index + 1: 0 is the "nothing pending" state that apply_pending waits on.
# mode 2 is free text, mode 3 is a number. The bounds are the row's own, so the
# form refuses out-of-range input instead of accepting it and clamping silently.
execute if data storage ra:settings cur{type:"str"} run scoreboard players set @s ra.input.mode 2
execute if data storage ra:settings cur{type:"str"} run scoreboard players set @s ra.input.min 0
execute if data storage ra:settings cur{type:"str"} run scoreboard players set @s ra.input.max 120

execute if data storage ra:settings cur{type:"int"} run scoreboard players set @s ra.input.mode 3
execute if data storage ra:settings cur{type:"int"} store result score @s ra.input.min run data get storage ra:settings cur.min
execute if data storage ra:settings cur{type:"int"} store result score @s ra.input.max run data get storage ra:settings cur.max

# Say that the request went out. ra_lib:input prints its own instructions once a
# backend opens, so if this line appears and nothing follows it, the failure is in
# the backend rather than in the settings code that asked for it.
function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open

# Start waiting ONLY now, and only if a backend actually opened -- see
# ra_settings:admin_edit_start. Row index + 1, because 0 is "nothing pending".
# Remember the request id as well as the fact that we are waiting, so the
# consumer can tell our answer from another tool's.
execute if score @s ra.input.backend matches 1..2 run scoreboard players operation @s ra.settings.req = @s ra.input.req
execute if score @s ra.input.backend matches 1..2 run scoreboard players operation @s ra.settings.pend = #v ra.set.tmp
execute if score @s ra.input.backend matches 1..2 run scoreboard players add @s ra.settings.pend 1

# Reported AFTER the backend is chosen, not before, so the numbers are the ones
# that were actually used. mode 2 is text and should pick backend 2 (the writable
# book); mode 3 is a number and should pick backend 1. A backend of 0 means
# nothing opened at all, and no amount of waiting will produce an answer.
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Waiting for your input",color:"gray"},{text:"  (mode ",color:"dark_gray"},{score:{name:"@s",objective:"ra.input.mode"},color:"dark_gray"},{text:", backend ",color:"dark_gray"},{score:{name:"@s",objective:"ra.input.backend"},color:"dark_gray"},{text:")",color:"dark_gray"}]
execute if score @s ra.input.backend matches 0 run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"No input method opened — nothing will arrive. Report the mode/backend above.",color:"red"}]
