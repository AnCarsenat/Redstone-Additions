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
scoreboard players operation @s ra.settings.pend = #v ra.set.tmp
scoreboard players add @s ra.settings.pend 1

# mode 2 is free text, mode 3 is a number. The bounds are the row's own, so the
# form refuses out-of-range input instead of accepting it and clamping silently.
execute if data storage ra:settings cur{type:"str"} run scoreboard players set @s ra.input.mode 2
execute if data storage ra:settings cur{type:"str"} run scoreboard players set @s ra.input.min 0
execute if data storage ra:settings cur{type:"str"} run scoreboard players set @s ra.input.max 120

execute if data storage ra:settings cur{type:"int"} run scoreboard players set @s ra.input.mode 3
execute if data storage ra:settings cur{type:"int"} store result score @s ra.input.min run data get storage ra:settings cur.min
execute if data storage ra:settings cur{type:"int"} store result score @s ra.input.max run data get storage ra:settings cur.max

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open
