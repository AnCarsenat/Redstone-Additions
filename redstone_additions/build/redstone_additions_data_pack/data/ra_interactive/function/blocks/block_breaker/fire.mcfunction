# /ra_interactive:blocks/block_breaker/fire
# Break the block in front. Context: as the marker, at the breaker, already
# confirmed powered and with something breakable in front.
#
# THE COOLDOWN IS RESET FIRST, AND THAT IS THE WHOLE POINT
# This used to be five separate `execute ... unless block ^ ^ ^1 #air ...` lines,
# with the reset on the last one. The first line destroys that block — so by the
# time the reset line re-tested `unless block ^ ^ ^1 #air`, the space WAS air, the
# condition was false, and the reset never ran. The counter climbed for ever, the
# `scores={ra.cooldown=20..}` gate was true on every tick from then on, and the
# breaker had no cooldown at all.
#
# Testing a condition once, acting on it, and never re-testing it afterwards is
# the fix. It is the same mistake the Clock's latch made in miniature.

scoreboard players set @s ra.cooldown 0

execute positioned ^ ^ ^1 unless block ~ ~ ~ #ra_lib:unbreakable run fill ~ ~ ~ ~ ~ ~ air destroy
playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 0.8
execute positioned ^ ^ ^1 run particle minecraft:block{block_state:"minecraft:stone"} ~ ~ ~ 0.3 0.3 0.3 0.1 20
