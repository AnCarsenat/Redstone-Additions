# /ra:tools/multimeter/report
# Print what this block is doing on its network.
# Context: as the target block's marker.

data modify storage ra:temp meter_found set value 1b

data modify storage ra:temp name_only set value 1b
function ra:tools/goggles/draw_block
data remove storage ra:temp name_only

tellraw @a[tag=ra.meter_active,limit=1] [{text:"── ",color:"dark_gray"},{nbt:"block_name",storage:"ra:temp",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]

# Not every custom block is on a network. Say so plainly rather than printing
# three zeroes that look like a flat battery.
execute unless entity @s[tag=ra.tr.node] run return run function ra:tools/multimeter/off_network

function ra_lib:transport/net/read

tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Network ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"   this block adds ",color:"gray"},{score:{name:"@s",objective:"ra.tr.cap"},color:"yellow"}]
tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Stored ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"green"},{text:" of ",color:"gray"},{score:{name:"#net_capacity",objective:"ra.tr.tmp"},color:"green"}]

# Electric grids are EU and fluid networks are millilitres; the medium name is
# the only thing that tells them apart from here, and an electric grid's medium
# is always "eu".
execute if data storage ra:transport cur.medium run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Holding ",color:"gray"},{nbt:"cur.medium",storage:"ra:transport",color:"aqua"}]
execute unless data storage ra:transport cur.medium run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Holding ",color:"gray"},{text:"nothing",color:"dark_gray"}]

# Whatever the block itself publishes for the goggles is worth repeating here:
# a consumer's draw, a generator's rate, a bridge's throughput.
execute if data entity @s data.properties.eu_use run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Draws ",color:"gray"},{nbt:"data.properties.eu_use",entity:"@s",color:"red"},{text:" EU per tick",color:"gray"}]
execute if data entity @s data.properties.generation_rate run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Makes ",color:"gray"},{nbt:"data.properties.generation_rate",entity:"@s",color:"green"},{text:" EU per tick",color:"gray"}]
execute if data entity @s data.properties.rate run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Moves ",color:"gray"},{nbt:"data.properties.rate",entity:"@s",color:"yellow"},{text:" per tick",color:"gray"}]
execute if data entity @s data.status.trend run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Change ",color:"gray"},{nbt:"data.status.trend",entity:"@s",color:"aqua"},{text:" per second",color:"gray"}]

playsound minecraft:block.note_block.bit block @a[tag=ra.meter_active,limit=1] ~ ~ ~ 0.6 1.8
