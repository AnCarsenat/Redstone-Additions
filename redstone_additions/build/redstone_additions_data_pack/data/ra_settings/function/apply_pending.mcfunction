# /ra_settings:apply_pending
# Write a typed value into the setting that was waiting for it.
# Context: as the player. Mirrors ra:tools/data_handler/apply_pending.
#
# A cancelled session -- the player dropped the form, or let it time out -- never
# becomes ready, so the wait has to be abandoned rather than held forever. The
# second condition matters: submit clears ra.input.active but leaves state at 2,
# and without it a finished answer would be thrown away on the very tick it
# arrived.

execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Input cancelled — nothing was changed.",color:"gray"}]
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run scoreboard players set @s ra.settings.pend 0
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return 0

execute store result score #ok ra.set.tmp run function ra_lib:input/poll
execute unless score #ok ra.set.tmp matches 2 run return 0

execute store result score #ok ra.set.tmp run function ra_lib:input/consume
execute unless score #ok ra.set.tmp matches 1 run return 0

# -1 means an operator typing into the admin tree: there is no menu row to write
# back to, and the target is named in storage ra:settings admin_edit instead.
# Act BEFORE clearing the flag: clearing it first would make the very next line's
# test false, and the operator's typed value would be consumed and dropped.
execute if score @s ra.settings.pend matches ..-1 run function ra_settings:admin_apply
execute if score @s ra.settings.pend matches ..-1 run scoreboard players set @s ra.settings.pend 0
execute unless score @s ra.settings.pend matches 1.. run return 0

execute store result storage ra:settings q.p int 1 run scoreboard players get @s ra.settings.page
scoreboard players operation #r ra.set.tmp = @s ra.settings.pend
scoreboard players remove #r ra.set.tmp 1
execute store result storage ra:settings q.r int 1 run scoreboard players get #r ra.set.tmp

scoreboard players set @s ra.settings.pend 0
function ra_settings:apply_at with storage ra:settings q
function ra_settings:page/open
