# /ra:tools/multimeter/media_one {m:"..."}
# Internal: one medium's display name and amount.
#
# The name falls back to the raw key, so a medium that is not in the registry
# shows as itself rather than as a blank.

$data modify storage ra:temp meter.name set value "$(m)"
$execute if data storage ra:wires media.$(m).name run data modify storage ra:temp meter.name set from storage ra:wires media.$(m).name
$execute store result score #meter.a ra.temp run data get storage ra:transport cur.amounts.$(m)

# EU is the one medium that is not a fluid and is deliberately not in the fluid
# registry -- a Creative Fluid Source set to "eu" would otherwise be accepted.
# So: anything the registry knows is measured in millilitres, and a grid's charge
# is measured in EU.
data modify storage ra:temp meter.unit set value " EU"
$execute if data storage ra:wires media.$(m) run data modify storage ra:temp meter.unit set value " mL"

execute if score #meter.n ra.temp matches 1.. run tellraw @a[tag=ra.meter_active,limit=1] [{text:"    ",color:"gray"},{nbt:"meter.name",storage:"ra:temp",color:"aqua"},{text:"  ",color:"gray"},{score:{name:"#meter.a",objective:"ra.temp"},color:"green"},{nbt:"meter.unit",storage:"ra:temp",color:"gray"}]
execute if score #meter.n ra.temp matches ..0 run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Holding ",color:"gray"},{nbt:"meter.name",storage:"ra:temp",color:"aqua"},{text:"  ",color:"gray"},{score:{name:"#meter.a",objective:"ra.temp"},color:"green"},{nbt:"meter.unit",storage:"ra:temp",color:"gray"}]
