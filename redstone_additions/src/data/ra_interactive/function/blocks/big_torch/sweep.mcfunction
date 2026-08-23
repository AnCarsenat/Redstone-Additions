# /ra_interactive:blocks/big_torch/sweep
# Deny one Big Torch's area to mobs that spawned inside it.
# Context: as the torch marker, at the torch position.
#
# HOW "SPAWNED IN" IS TOLD FROM "WALKED IN"
# A data pack cannot stop a spawn from happening; it can only remove what
# appeared. Removing every hostile mob inside the radius would be a different
# block entirely -- it would clear mobs that walked in from outside, which is a
# mob grinder, not a torch.
#
# So every mob in a band REACHING PAST the radius is remembered with
# ra.big_torch.seen, and only unremembered mobs inside the radius are removed.
# Anything approaching on foot crosses the outer band first and is remembered
# there, so it survives crossing into the radius. Anything that spawns inside
# appears with no tag at all and is denied on the next sweep.
#
# The order matters: deny first, remember second. Doing it the other way round
# would tag a mob in the same sweep it spawned in and never deny anything.
#
# The band is 16 blocks wide against a 10-tick sweep. The fastest thing in the
# tag covers about 5 blocks in that time, so nothing can cross the band unseen.

function ra_lib:util/property {name:"radius",default:16,min:1}

# THE 100-BLOCK CEILING IS WRITTEN BACK, NOT ONLY APPLIED
# The selector cost is the reason for it: `distance` describes a sphere, so
# doubling the radius is eight times the volume to search, and this runs per
# torch every ten ticks.
#
# Clamping the working value alone was not enough. `data.properties.radius` kept
# whatever was typed, and the goggles read that path directly -- so a torch set
# to 500 advertised `Radius: 500 blocks` while sweeping 100, and the Data Handler
# offered the same 500 back for editing. The stored value is corrected here so
# every reader of it agrees with what the block actually does.
# The write-back goes first: it tests the value as typed, and the clamp below is
# what would otherwise have already hidden it.
execute if score #prop ra.temp matches 101.. run data modify entity @s data.properties.radius set value 100
execute if score #prop ra.temp matches 101.. run scoreboard players set #prop ra.temp 100

execute store result storage ra:temp big_torch.radius int 1 run scoreboard players get #prop ra.temp
scoreboard players operation #prop.outer ra.temp = #prop ra.temp
scoreboard players add #prop.outer ra.temp 16
execute store result storage ra:temp big_torch.outer int 1 run scoreboard players get #prop.outer ra.temp

function ra_interactive:blocks/big_torch/sweep_at with storage ra:temp big_torch
