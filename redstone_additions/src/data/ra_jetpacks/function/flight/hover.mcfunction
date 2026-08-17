# /ra_jetpacks:flight/hover
# Hover mode: sneak steers up or down by where you look, and letting go holds.
# Context: as the player wearing the jetpack, at the player.
#
# Descending is NOT levitation. Amplifier 255 is stored as 255, not as -1, so the
# old wrap-around trick does not sink a player — it throws them upward at about
# twelve blocks a second. Sinking hands gravity back with slow falling on top,
# which keeps the descent gentle and eats the fall damage.
#
# Holding station is a servo, not an absence of gravity: the vertical speed is
# measured from one tick to the next and the gravity attribute is aimed against
# it. There is no command that clears a player's velocity — /tp used to, but
# MC-275455 was fixed in the 1.21.2 snapshots and relative teleports now keep
# motion, so an in-place tp does nothing at all. Two thruster strengths against
# a small dead zone kill a levitation coast in two or three ticks, which is what
# makes hover feel abrupt instead of icy.
#
# Pitch: -90 is straight up, +90 is straight down.

# Vertical speed, in thousandths of a block per tick, from the change in Pos[1].
execute store result score #jp.y ra.temp run data get entity @s Pos[1] 1000
execute unless score @s ra.jp.y = @s ra.jp.y run scoreboard players operation @s ra.jp.y = #jp.y ra.temp
scoreboard players operation #jp.dy ra.temp = #jp.y ra.temp
scoreboard players operation #jp.dy ra.temp -= @s ra.jp.y
scoreboard players operation @s ra.jp.y = #jp.y ra.temp

execute store result score @s ra.pitch run data get entity @s Rotation[1] 1

# -1 sink, 0 hold, 1 climb. The dead zone is wide on purpose: a narrow one made
# every small look-around flip the state.
scoreboard players set #jp.dir ra.temp 0
execute if predicate ra:is_sneaking if score @s ra.pitch matches ..-30 run scoreboard players set #jp.dir ra.temp 1
execute if predicate ra:is_sneaking if score @s ra.pitch matches 30.. run scoreboard players set #jp.dir ra.temp -1

# Standing on something hands the player straight back to vanilla: walking around
# with gravity switched off feels wrong, and the servo has nothing to hold up. A
# block below the feet at ~-0.1 means grounded — with the feet at y=64.0 that
# reads block 63, and one tick off the floor it reads air. Water and lava count as
# grounded too, which is fine: swimming should be swimming. Sneak plus look up is
# exempt, or hover could never leave the ground.
#
# ra.jp.air records that the player has actually been airborne, so flight/grounded
# can tell a landing from someone who is simply standing there.
execute if block ~ ~-0.1 ~ #minecraft:air run scoreboard players set @s ra.jp.air 1
execute unless block ~ ~-0.1 ~ #minecraft:air if score #jp.dir ra.temp matches ..0 run function ra_jetpacks:flight/grounded
execute unless block ~ ~-0.1 ~ #minecraft:air if score #jp.dir ra.temp matches ..0 run return 0

# Sinking: vanilla gravity back, capped by slow falling.
execute if score #jp.dir ra.temp matches -1 run scoreboard players set #jp.tier ra.temp 9
execute if score #jp.dir ra.temp matches -1 run effect clear @s minecraft:levitation
execute if score #jp.dir ra.temp matches -1 run effect give @s minecraft:slow_falling 1 0 true

# Climbing: levitation drives the ascent, so gravity only has to stay out of the
# way. Amplifier 2 is three blocks a second, matching the descent.
execute if score #jp.dir ra.temp matches 1 run scoreboard players set #jp.tier ra.temp 0
execute if score #jp.dir ra.temp matches 1 run effect clear @s minecraft:slow_falling
execute if score #jp.dir ra.temp matches 1 run effect give @s minecraft:levitation 1 2 true

# Holding: the servo picks the tier.
execute if score #jp.dir ra.temp matches 0 run effect clear @s minecraft:slow_falling
execute if score #jp.dir ra.temp matches 0 run effect clear @s minecraft:levitation
execute if score #jp.dir ra.temp matches 0 run function ra_jetpacks:flight/hold

function ra_jetpacks:flight/gravity

# Idle wash: the thrusters are holding the player's weight even standing still,
# so the smoke never stops. Count 0 turns the delta into a velocity, which is the
# only way to make campfire smoke fall instead of rise.
particle minecraft:campfire_cosy_smoke ~0.15 ~-0.2 ~0.15 0 -1 0 0.08 0
particle minecraft:campfire_cosy_smoke ~-0.15 ~-0.2 ~-0.15 0 -1 0 0.08 0

# Under thrust: the same smoke, more of it and pushed harder. No end rods — the
# white streaks read as glitch geometry rather than exhaust.
execute unless score #jp.dir ra.temp matches 0 run particle minecraft:campfire_cosy_smoke ~0.15 ~-0.25 ~-0.15 0 -1 0 0.22 0
execute unless score #jp.dir ra.temp matches 0 run particle minecraft:campfire_cosy_smoke ~-0.15 ~-0.25 ~0.15 0 -1 0 0.22 0
execute unless score #jp.dir ra.temp matches 0 run particle minecraft:campfire_cosy_smoke ~ ~-0.25 ~ 0 -1 0 0.3 0
execute unless score #jp.dir ra.temp matches 0 run particle minecraft:smoke ~ ~-0.15 ~ 0.12 0.02 0.12 0.02 2

# The engine runs the whole time hover is holding the player up, hold included —
# that is the thrusters carrying their weight.
function ra_jetpacks:flight/sound

# Holding station costs fuel too — the jetpack is carrying the player's weight.
# Sinking is free.
execute if score #jp.dir ra.temp matches 0.. run function ra_jetpacks:flight/fuel
