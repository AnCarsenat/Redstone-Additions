# /ra_jetpacks:flight/hold
# Pick a thruster tier that cancels the player's vertical speed.
# Context: as the player. Reads #jp.dy ra.temp (thousandths of a block per tick),
# writes #jp.tier ra.temp for ra_jetpacks:flight/gravity.
#
# Dead zone is 0.006 blocks a tick, about a tenth of a block a second — below
# what anyone can see, and wide enough that the tier is not flapping every tick.
# The gentle tier changes speed by 0.02 a tick and the hard tier by 0.08, so a
# levitation coast (0.15 a tick) is gone in two ticks and small residues settle
# without overshooting into a visible bob.

scoreboard players set #jp.tier ra.temp 0
execute if score #jp.dy ra.temp matches 6..60 run scoreboard players set #jp.tier ra.temp 1
execute if score #jp.dy ra.temp matches 61.. run scoreboard players set #jp.tier ra.temp 2
execute if score #jp.dy ra.temp matches -60..-6 run scoreboard players set #jp.tier ra.temp -1
execute if score #jp.dy ra.temp matches ..-61 run scoreboard players set #jp.tier ra.temp -2
