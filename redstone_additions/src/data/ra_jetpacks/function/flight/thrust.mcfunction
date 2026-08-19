# /ra_jetpacks:flight/thrust
# Thruster kit: actually make the player go faster horizontally.
# Context: as the player, at the player. Only called while airborne.
#
# WHY THIS IS NOT AN ATTRIBUTE
# It was `minecraft:movement_speed`, +120%, and it did nothing you could feel.
# That attribute governs walking. Once you are off the ground, horizontal
# movement is driven by a much smaller air-control factor instead, so raising
# walk speed while flying raises a number that is barely being read. It is not a
# Minecraft bug, it is the wrong lever.
#
# The lever that works is displacement. Measure how far the player actually moved
# horizontally last tick and add a fraction of it back on top, which means:
#   - standing still in hover does nothing at all, because the delta is zero
#   - it pushes whichever way you are ACTUALLY going, so strafing and flying
#     backwards get the same help as flying forwards, and looking around does
#     not veer you off course the way a look-direction shove would
#
# It feeds on its own output -- next tick's delta includes this tick's push -- so
# it accelerates rather than jumping to speed, and the cap is what stops that
# being a runaway. Let go and air drag shrinks the delta, so the boost fades with
# it instead of having to be switched off.

execute store result score #jp.px ra.temp run data get entity @s Pos[0] 1000
execute store result score #jp.pz ra.temp run data get entity @s Pos[2] 1000

# No previous sample on the first tick after take-off. A score that has never
# been set is ABSENT, and an absent score does not even equal itself.
execute unless score @s ra.jp.x = @s ra.jp.x run scoreboard players operation @s ra.jp.x = #jp.px ra.temp
execute unless score @s ra.jp.z = @s ra.jp.z run scoreboard players operation @s ra.jp.z = #jp.pz ra.temp

scoreboard players operation #jp.mx ra.temp = #jp.px ra.temp
scoreboard players operation #jp.mx ra.temp -= @s ra.jp.x
scoreboard players operation #jp.mz ra.temp = #jp.pz ra.temp
scoreboard players operation #jp.mz ra.temp -= @s ra.jp.z

scoreboard players operation @s ra.jp.x = #jp.px ra.temp
scoreboard players operation @s ra.jp.z = #jp.pz ra.temp

# SMOOTHING, BECAUSE RAW DELTAS JITTER
# Pushing a fraction of last tick's raw movement made the jetpack shake. A
# single tick's delta is noisy -- it carries the player's own input, collision
# nudges, and the previous tick's push -- so feeding it straight back turned
# every wobble into a teleport of a different size, and a teleport of a
# different size every tick is exactly what the client renders as jitter.
#
# So the delta feeds a running average instead of the push directly:
#     smoothed = (smoothed * 3 + delta) / 4
# which is a low-pass filter with a time constant of about four ticks. The
# player's actual movement still drives it, but the number the push is built
# from changes gradually, and gradual is what stops it looking like shaking.
#
# It also fixes the runaway from the other direction: the smoothed value cannot
# jump, so the acceleration curve is smooth rather than compounding tick by tick.
execute unless score @s ra.jp.vx = @s ra.jp.vx run scoreboard players set @s ra.jp.vx 0
execute unless score @s ra.jp.vz = @s ra.jp.vz run scoreboard players set @s ra.jp.vz 0

scoreboard players set #jp.three ra.temp 3
scoreboard players set #jp.four ra.temp 4

scoreboard players operation @s ra.jp.vx *= #jp.three ra.temp
scoreboard players operation @s ra.jp.vx += #jp.mx ra.temp
scoreboard players operation @s ra.jp.vx /= #jp.four ra.temp
scoreboard players operation @s ra.jp.vz *= #jp.three ra.temp
scoreboard players operation @s ra.jp.vz += #jp.mz ra.temp
scoreboard players operation @s ra.jp.vz /= #jp.four ra.temp

# The push is a fraction of the SMOOTHED speed, not of this tick's delta.
scoreboard players operation #jp.mx ra.temp = @s ra.jp.vx
scoreboard players operation #jp.mz ra.temp = @s ra.jp.vz

# Configurable, but read once per tick in ra_jetpacks:tick rather than here --
# this function runs for every player in the air.
scoreboard players operation #jp.k ra.temp = #jp.cfg.k ra.temp
scoreboard players set #jp.c100 ra.temp 100
scoreboard players operation #jp.mx ra.temp *= #jp.k ra.temp
scoreboard players operation #jp.mx ra.temp /= #jp.c100 ra.temp
scoreboard players operation #jp.mz ra.temp *= #jp.k ra.temp
scoreboard players operation #jp.mz ra.temp /= #jp.c100 ra.temp

# Cap the push at 0.35 blocks a tick each way, so amplifying our own
# amplification settles at a top speed instead of climbing until the player
# outruns the chunk loader.
scoreboard players operation #jp.cap ra.temp = #jp.cfg.cap ra.temp
# The negative bound is derived, not configured twice: two numbers that must stay
# equal and opposite are one number and a sign.
scoreboard players set #jp.neg ra.temp -1
scoreboard players operation #jp.ncap ra.temp = #jp.cap ra.temp
scoreboard players operation #jp.ncap ra.temp *= #jp.neg ra.temp
execute if score #jp.mx ra.temp > #jp.cap ra.temp run scoreboard players operation #jp.mx ra.temp = #jp.cap ra.temp
execute if score #jp.mx ra.temp < #jp.ncap ra.temp run scoreboard players operation #jp.mx ra.temp = #jp.ncap ra.temp
execute if score #jp.mz ra.temp > #jp.cap ra.temp run scoreboard players operation #jp.mz ra.temp = #jp.cap ra.temp
execute if score #jp.mz ra.temp < #jp.ncap ra.temp run scoreboard players operation #jp.mz ra.temp = #jp.ncap ra.temp

# IT ENGAGES ON SPRINT, NOT ON SPEED
# A data pack cannot set a player's velocity. The only server-side ways to move a
# player are teleporting, knockback and vehicles -- so a continuous boost has to
# be a teleport a tick, and that reads as jitter no matter how well the magnitude
# is smoothed. That is the mechanism, not the tuning. What can be chosen is WHEN
# it fires.
#
# The first attempt was a speed floor: engage above 0.18 blocks a tick. That was
# the wrong signal twice over. In classic mode you fly by HOLDING SNEAK, and
# horizontal movement in the air never gets near 0.18 -- air control is a small
# fraction of walking speed -- so the thruster simply never switched on. And a
# speed floor cannot tell crossing terrain from lining up a block, because both
# can be slow.
#
# Sprint is the signal, and it is better than a threshold on every count: it is
# an explicit thing the player does, it means "go fast" in every other context in
# the game, it works identically in both flight modes, and nobody sprints while
# placing blocks -- which is exactly where the jitter was unwelcome.
execute unless predicate ra:is_sprinting run tag @s remove ra.jp.thrusting
execute unless predicate ra:is_sprinting run return 0
tag @s add ra.jp.thrusting

# Sprinting on the spot still measures a delta of nearly nothing. Below a couple
# of centimetres a tick the push is not felt, but it IS still a teleport, so
# under the threshold do nothing at all.
scoreboard players operation #jp.dead ra.temp = #jp.cfg.dead ra.temp
scoreboard players set #jp.neg ra.temp -1
scoreboard players operation #jp.ndead ra.temp = #jp.dead ra.temp
scoreboard players operation #jp.ndead ra.temp *= #jp.neg ra.temp
scoreboard players set #jp.moving ra.temp 0
execute if score #jp.mx ra.temp > #jp.dead ra.temp run scoreboard players set #jp.moving ra.temp 1
execute if score #jp.mx ra.temp < #jp.ndead ra.temp run scoreboard players set #jp.moving ra.temp 1
execute if score #jp.mz ra.temp > #jp.dead ra.temp run scoreboard players set #jp.moving ra.temp 1
execute if score #jp.mz ra.temp < #jp.ndead ra.temp run scoreboard players set #jp.moving ra.temp 1
execute if score #jp.moving ra.temp matches 0 run return 0

execute store result storage ra:jetpacks push.dx double 0.001 run scoreboard players get #jp.mx ra.temp
execute store result storage ra:jetpacks push.dz double 0.001 run scoreboard players get #jp.mz ra.temp
function ra_jetpacks:flight/thrust_push with storage ra:jetpacks push
