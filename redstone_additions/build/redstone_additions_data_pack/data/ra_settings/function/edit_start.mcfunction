# /ra_settings:edit_start
# Ask the player to type a value for the edit described in storage ra:settings edit.
# Context: as the player.
#
# MODELLED ON ra:tools/data_handler/generic_action, DELIBERATELY
# The Data Handler's text input works. Every difference between this and that was
# a difference this had to justify, and none of them could:
#
#   - The pending flag is set BEFORE the session opens, not after. Waiting until
#     after meant the flag depended on a backend having been chosen, which is one
#     more thing that can be false for reasons unrelated to whether we asked.
#   - What is being edited lives in STORAGE, as one compound, the way ra:dh keeps
#     pending_name and pending_kind. It used to be a scoreboard index into the
#     menu row list, which is only meaningful while that list is unchanged.
#   - The caller fills in storage ra:settings edit and calls this. Nothing here
#     knows whether the target is a global key, a block property or a player's
#     own preference.
#
# storage ra:settings edit:
#   {kind:"global"|"prop"|"user", type:"str"|"int",
#    key/block+prop/obj as the kind requires, min:N, max:N}

scoreboard players set @s ra.settings.pend 1

function ra_lib:input/cancel

scoreboard players set @s ra.input.mode 2
scoreboard players set @s ra.input.min 0
scoreboard players set @s ra.input.max 120

execute if data storage ra:settings edit{type:"int"} run scoreboard players set @s ra.input.mode 3
execute if data storage ra:settings edit{type:"int"} store result score @s ra.input.min run data get storage ra:settings edit.min
execute if data storage ra:settings edit{type:"int"} store result score @s ra.input.max run data get storage ra:settings edit.max

function ra_lib:input/session/create
function ra_lib:input/router/select_backend
function ra_lib:input/router/open

# Which request is ours. ra_lib:input is shared with the Data Handler, and
# consume() tears a session down as it reads, so whoever consumes must first
# check that the answer was meant for them.
scoreboard players operation @s ra.settings.req = @s ra.input.req
