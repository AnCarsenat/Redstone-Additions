# /ra_ender:debug/one {kind}
# One vault's report. Context: as its marker, at the block.
#
# The channel's *type* is the first thing printed, because a channel that is not a
# string matches nothing and is invisible in chat: 5 and "5" print identically.

data modify storage ra:ender dbg set value {}
data modify storage ra:ender dbg.pos set from entity @s Pos
data modify storage ra:ender dbg.props set from entity @s data.properties

execute store success score #ender.isstr ra.temp run data modify storage ra:ender probe set string entity @s data.properties.channel

$tellraw @s [{text:"— $(kind) Vault ",color:"gray"},{nbt:"dbg.pos",storage:"ra:ender",color:"white"}]
tellraw @s [{text:"   props ",color:"gray"},{nbt:"dbg.props",storage:"ra:ender",color:"white"}]
execute if score #ender.isstr ra.temp matches 1 run tellraw @s [{text:"   channel is a string",color:"green"}]
execute if score #ender.isstr ra.temp matches 0 run tellraw @s [{text:"   channel is NOT a string — it matches nothing until repaired",color:"red"}]

# Who this vault can currently reach: the tags are rebuilt every tick from mode.
execute if entity @s[tag=ra.ender.recv_item] run tellraw @s [{text:"   can receive items",color:"aqua"}]
execute if entity @s[tag=ra.ender.send_item] run tellraw @s [{text:"   can be pulled from",color:"aqua"}]
execute if entity @s[tag=ra.ender.recv_fluid] run tellraw @s [{text:"   can receive fluid",color:"aqua"}]
execute if entity @s[tag=ra.ender.recv_power] run tellraw @s [{text:"   can receive EU",color:"aqua"}]
