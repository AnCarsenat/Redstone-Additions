# /ra_settings:apply_pending
# Write a typed value into whatever storage ra:settings edit describes.
# Context: as the player.
#
# The guard order is the Data Handler's, for the same reasons its comments give:
# a cancelled session never becomes ready, so waiting on one forever is wrong;
# and submit clears ra.input.active while leaving state at 2, so testing only the
# tag would throw away a finished answer on the tick it arrived.

execute unless data storage ra:settings edit run scoreboard players set @s ra.settings.pend 0
execute unless data storage ra:settings edit run return 0

execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run scoreboard players set @s ra.settings.pend 0
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return 0

# Not our answer. The Data Handler shares this library, and consuming its session
# would hand it an empty result and leave it waiting for one already taken.
execute unless score @s ra.settings.req = @s ra.input.req run return 0

execute store result score #ok ra.set.tmp run function ra_lib:input/poll
execute unless score #ok ra.set.tmp matches 2 run return 0

execute store result score #ok ra.set.tmp run function ra_lib:input/consume
execute unless score #ok ra.set.tmp matches 1 run return 0

scoreboard players set @s ra.settings.pend 0
function ra_settings:apply_edit
