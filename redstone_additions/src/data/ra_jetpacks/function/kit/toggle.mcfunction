# /ra_jetpacks:kit/toggle {mute,label}
# Switch one fitted upgrade off or on without removing it. Context: as the player.
#
# THE STATE IS READ BEFORE ANYTHING CHANGES IT
# This was two lines: remove the tag if present, then add it if absent. The
# second line runs AFTER the first, sees the tag the first line just removed, and
# puts it straight back -- so every click ended muted and a kit could be switched
# off but never on again. The same shape broke the Block Breaker's cooldown once
# already: a condition re-tested after your own first line has invalidated it.
#
# WHY "OFF" IS ON THE PLAYER AND "FITTED" IS ON THE ITEM
# Being fitted is a property of the jetpack -- it survives being handed to
# someone else, and it cost a kit. Being switched off is a preference of whoever
# is wearing it, in the same family as /trigger ra.jp.sound and ra.jp.power.
# It is also the only place it can go cheaply: fitted state is one generated item
# modifier per reachable combination, and an on/off flag per kit would take that
# from sixteen files to a hundred and twenty-eight.

scoreboard players set #jp.was ra.temp 0
$execute if entity @s[tag=$(mute)] run scoreboard players set #jp.was ra.temp 1

$execute if score #jp.was ra.temp matches 1 run tag @s remove $(mute)
$execute if score #jp.was ra.temp matches 0 run tag @s add $(mute)

$execute if score #jp.was ra.temp matches 1 run title @s actionbar [{text:"$(label) switched on",color:"green"}]
$execute if score #jp.was ra.temp matches 0 run title @s actionbar [{text:"$(label) switched off",color:"gray"}]
playsound minecraft:block.lever.click player @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.7 1.4
