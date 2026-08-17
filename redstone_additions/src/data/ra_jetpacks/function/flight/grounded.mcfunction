# /ra_jetpacks:flight/grounded
# Hover mode with a block under the feet.
# Context: as the player, at the player.
#
# Two different situations land here, and telling them apart is the whole point:
#
#   standing, never took off   -> vanilla physics, thrusters idle, jetpack stays on
#                                 so sneak plus look up can still lift off
#   just landed (ra.jp.air 1)  -> switch off, the same as /trigger ra.jp.power
#
# Switching off for merely standing made hover unusable: the jetpack turned itself
# off the tick after the mode was selected, so there was never an armed jetpack to
# take off with.

# Vanilla gravity while on the ground, and no leftover flight effects or engine.
scoreboard players set #jp.tier ra.temp 9
function ra_jetpacks:flight/gravity
effect clear @s minecraft:levitation
effect clear @s minecraft:slow_falling
function ra_jetpacks:flight/sound_off

# Forget the sampled height, so the first airborne tick measures a fresh speed
# instead of a jump's worth of it.
scoreboard players reset @s ra.jp.y

# Touchdown. power_toggle *toggles*, so it is only called while the jetpack is on.
execute if score @s ra.jp.air matches 1 unless score @s ra.jp.off matches 1 run function ra_jetpacks:mode/power_toggle
scoreboard players set @s ra.jp.air 0
