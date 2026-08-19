# /ra_settings:admin_dispatch
# A server-settings button was clicked. Context: as the player.
#
# The tag is re-checked here even though the objective is only enabled for
# players who hold it. Enabling is per-player state that survives the tag being
# taken away, so without this a session that expired mid-click would still run
# its last action.

execute store result score #a ra.set.tmp run scoreboard players get @s ra.settings.admin
scoreboard players set @s ra.settings.admin 0

execute unless entity @s[tag=ra.admin] run return run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Your server-settings session has ended.",color:"red"}]

scoreboard players enable @s ra.settings.admin
scoreboard players set @s ra.admin.ttl 6000

# Codes are 1-based, because a trigger score of 0 is the "not clicked" state.
scoreboard players remove #a ra.set.tmp 1
execute store result storage ra:settings q.a int 1 run scoreboard players get #a ra.set.tmp
function ra_settings:admin_run with storage ra:settings q
