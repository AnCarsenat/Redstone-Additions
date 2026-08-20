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

# The issue asks for a 100-block ceiling, and the selector cost is why: distance
# is a sphere, so doubling the radius is eight times the volume to search.
execute if score #prop ra.temp matches 101.. run scoreboard players set #prop ra.temp 100

execute store result storage ra:temp big_torch.radius int 1 run scoreboard players get #prop ra.temp
scoreboard players operation #prop.outer ra.temp = #prop ra.temp
scoreboard players add #prop.outer ra.temp 16
execute store result storage ra:temp big_torch.outer int 1 run scoreboard players get #prop.outer ra.temp

function ra_interactive:blocks/big_torch/sweep_at with storage ra:temp big_torch
