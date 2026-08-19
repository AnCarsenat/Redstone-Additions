# /ra_jetpacks:flight/tick_player
# One player wearing a jetpack.
# Context: as the player, at the player.

scoreboard players enable @s ra.jp.mode
scoreboard players enable @s ra.jp.sound
scoreboard players enable @s ra.jp.power
scoreboard players enable @s ra.jp.kits

# Switched off with /trigger ra.jp.power: hand anything left over back to vanilla
# and stay out of the way. The triggers above still work, so it can be switched on.
execute if score @s ra.jp.off matches 1 if entity @s[tag=ra.jetpack_on] run function ra_jetpacks:flight/stop
execute if score @s ra.jp.off matches 1 run return 0

tag @s add ra.jetpack_on

# THE CHESTPLATE IS THE RECORD; THESE TAGS ARE A CACHE OF IT
# Re-derived every tick rather than set once when a kit is fitted, so taking the
# jetpack off, handing it to someone else or dying with it all do the right thing
# with no extra bookkeeping. It also self-heals worlds where the upgrades were
# briefly stored on the player: those tags are cleared here the first time a
# chestplate that does not carry the flag is worn.
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_speed:1b}}] unless entity @s[tag=ra.jp.mute_speed] run tag @s add ra.jp.kit_speed
execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_speed:1b}}] run tag @s remove ra.jp.kit_speed
execute if entity @s[tag=ra.jp.mute_speed] run tag @s remove ra.jp.kit_speed
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_lift:1b}}] unless entity @s[tag=ra.jp.mute_lift] run tag @s add ra.jp.kit_lift
execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_lift:1b}}] run tag @s remove ra.jp.kit_lift
execute if entity @s[tag=ra.jp.mute_lift] run tag @s remove ra.jp.kit_lift
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_scorch:1b}}] unless entity @s[tag=ra.jp.mute_scorch] run tag @s add ra.jp.kit_scorch
execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_scorch:1b}}] run tag @s remove ra.jp.kit_scorch
execute if entity @s[tag=ra.jp.mute_scorch] run tag @s remove ra.jp.kit_scorch

# Both modes are driven by holding sneak, and sneaking normally costs 70% of
# walking speed — which is why classic mode felt like wading. A jetpack pins
# sneaking_speed (base 0.3) back up to a full 1.0 while it is worn.
execute unless entity @s[tag=ra.jp.speed_on] run attribute @s minecraft:sneaking_speed modifier add ra.jetpack.sneak 0.7 add_value
tag @s add ra.jp.speed_on

# Airborne, computed once and read by both the Thruster and the ground-reset
# below. This lives here rather than inside flight/thrust because more than one
# thing needs it and the test is a block read.
function ra_jetpacks:flight/airborne

# Thruster kit: the horizontal upgrade, and ONLY while off the ground. It used to
# apply on the ground too, which made a fitted jetpack a permanent sprint boost
# you could not take off without removing the chestplate.
#
# It also used to be an attribute modifier, which is the wrong lever entirely --
# see flight/thrust for why movement_speed does almost nothing once you are in
# the air.
execute if score #jp.airborne ra.temp matches 0 run scoreboard players reset @s ra.jp.x
execute if score #jp.airborne ra.temp matches 0 run scoreboard players reset @s ra.jp.z
execute if score #jp.airborne ra.temp matches 0 run scoreboard players reset @s ra.jp.vx
execute if score #jp.airborne ra.temp matches 0 run scoreboard players reset @s ra.jp.vz
execute if score #jp.airborne ra.temp matches 0 run tag @s remove ra.jp.thrusting

# A jetpack that ran dry comes back the moment there is fuel again.
execute if entity @s[tag=ra.jp.dry] if items entity @s container.* minecraft:coal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] if items entity @s weapon.offhand minecraft:coal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] if items entity @s container.* minecraft:charcoal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] if items entity @s weapon.offhand minecraft:charcoal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] run return 0

execute if score @s ra.jp.state matches 1 run function ra_jetpacks:flight/hover
execute unless score @s ra.jp.state matches 1 run function ra_jetpacks:flight/classic

# Scorch kit. Only while actually off the ground -- standing in a village with
# the kit fitted should not set fire to the villagers.
# IS THE JETPACK ACTUALLY RUNNING?
# The upgrade kits used to fire on "wearing the kit and off the ground", which is
# not the same thing at all. In normal mode the jetpack only runs while you sneak
# -- flight/classic returns early otherwise -- so jump, sprint, jump handed you the
# thruster for free, with the pack switched off. In hover the jetpack IS holding
# you up whenever you are airborne, so there the two really do coincide.
scoreboard players set #jp.firing ra.temp 0
execute if score #jp.airborne ra.temp matches 1 if score @s ra.jp.state matches 1 run scoreboard players set #jp.firing ra.temp 1
execute if score #jp.airborne ra.temp matches 1 unless score @s ra.jp.state matches 1 if predicate ra:is_sneaking run scoreboard players set #jp.firing ra.temp 1

# Below the fuel check on purpose. Both kits used to run above it, so they kept
# working on an empty tank -- the one state where the jetpack is unambiguously off.
# Not firing means the smoothing has to be dropped, not merely paused. thrust
# measures how far you moved since it last looked; leave that reading behind while
# the pack is off and the next burst opens with a position delta covering the
# whole glide, which is a lurch nobody asked for. Cleared, it re-seeds on the next
# tick with a delta of zero -- the same thing that happens on landing.
execute if score #jp.firing ra.temp matches 0 run scoreboard players reset @s ra.jp.x
execute if score #jp.firing ra.temp matches 0 run scoreboard players reset @s ra.jp.z
execute if score #jp.firing ra.temp matches 0 run scoreboard players reset @s ra.jp.vx
execute if score #jp.firing ra.temp matches 0 run scoreboard players reset @s ra.jp.vz
execute if score #jp.firing ra.temp matches 0 run tag @s remove ra.jp.thrusting

execute if entity @s[tag=ra.jp.kit_speed] if score #jp.firing ra.temp matches 1 run function ra_jetpacks:flight/thrust
execute if entity @s[tag=ra.jp.kit_scorch] if score #jp.firing ra.temp matches 1 run function ra_jetpacks:flight/scorch
