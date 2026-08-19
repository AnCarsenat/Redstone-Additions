# /ra_settings:act
# A row button was clicked. Context: as the player.
#
# The code is the row index plus one, offset by a band that says WHICH button:
#   +0     the row's only button, or its "down"      -> direction -1
#   +512   the row's "up"                            -> direction +1
#   +1024  "Modify", which opens a typed input       -> direction  0
# The bands are far past any real row count, so splitting them apart is two
# comparisons rather than a second score carried alongside.
#
# Plus one because /trigger cannot deliver 0: a trigger score of 0 is exactly the
# "not clicked" state the dispatcher waits on, so row 0 would be a dead button.

execute store result score #v ra.set.tmp run scoreboard players get @s ra.settings.act
scoreboard players set @s ra.settings.act 0
scoreboard players enable @s ra.settings.act

# Highest band first: 1025.. would otherwise also satisfy the 513.. test.
scoreboard players set #dir ra.set.tmp -1
execute if score #v ra.set.tmp matches 1025.. run scoreboard players set #dir ra.set.tmp 0
execute if score #v ra.set.tmp matches 1025.. run scoreboard players remove #v ra.set.tmp 1024
execute if score #v ra.set.tmp matches 513..1024 run scoreboard players set #dir ra.set.tmp 1
execute if score #v ra.set.tmp matches 513..1024 run scoreboard players remove #v ra.set.tmp 512
scoreboard players remove #v ra.set.tmp 1

execute store result storage ra:settings q.p int 1 run scoreboard players get @s ra.settings.page
execute store result storage ra:settings q.r int 1 run scoreboard players get #v ra.set.tmp
function ra_settings:act/at with storage ra:settings q
