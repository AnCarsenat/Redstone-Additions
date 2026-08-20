# /ra_settings:admin_dispatch
# A server-settings button was clicked. Context: as the player.
#
# The tag is re-checked here even though the objective is only enabled for
# players who hold it. Enabling is per-player state that OUTLIVES the tag: a
# player whose access was revoked still has an armed trigger until something
# resets it, and this is what stops that last armed click from working.

execute store result score #a ra.set.tmp run scoreboard players get @s ra.settings.admin
scoreboard players set @s ra.settings.admin 0

execute unless entity @s[tag=ra.admin] run return run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"You do not have server-settings access.",color:"red"}]

scoreboard players enable @s ra.settings.admin

# 1 means "open the index" -- what a bare /trigger ra.settings.admin produces.
execute if score #a ra.set.tmp matches 1 run return run function ra_settings:admin/show

# Everything else is an action, numbered from 2 so that 1 could be reserved.
scoreboard players remove #a ra.set.tmp 2
execute store result storage ra:settings q.a int 1 run scoreboard players get #a ra.set.tmp
function ra_settings:admin_run with storage ra:settings q
