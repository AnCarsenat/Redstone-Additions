# /ra:tools/multimeter/media
# Internal: say what the network just read is holding, medium by medium.
# Context: as the target block's marker, immediately after ra_lib:transport/net/read.
#
# The goggles cannot do this. A billboard is one short line read from across the
# room, so a mixed run reads "Multimedium" there and the numbers live here --
# which is what the Multimeter is for: stand in front of one block and get the
# whole answer in chat, where it can take as many lines as it needs.

data remove storage ra:temp meterq
data modify storage ra:temp meterq.queue set from storage ra:transport cur.media

# One medium reads as a sentence. Several want a heading and a line each, so the
# amounts line up under it instead of running together.
scoreboard players set #meter.n ra.temp 0
execute if data storage ra:temp meterq.queue[1] run scoreboard players set #meter.n ra.temp 1
execute if score #meter.n ra.temp matches 1.. run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Holding",color:"gray"}]

# A network with a primary and no list should not exist -- ra_lib:transport/net
# /read backfills one -- but naming it is better than printing nothing at all.
execute unless data storage ra:temp meterq.queue[0] run return run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Holding ",color:"gray"},{nbt:"cur.medium",storage:"ra:transport",color:"aqua"}]

function ra:tools/multimeter/media_next

data remove storage ra:temp meterq
data remove storage ra:temp meter
