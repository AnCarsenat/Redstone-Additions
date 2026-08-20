# /ra_wires:blocks/electric_furnace/refresh_display
# Rebuild the Electric Furnace's skin, lit while it is working.
# Context: as the marker, at the block.
#
# Same arrangement as the EU Generator: a barrel wearing a furnace, because the
# mechanics wanted are a plain 27-slot inventory and the appearance wanted is a
# furnace. Through apply_lit, since a block state cannot ride in the block's name
# -- see ra_lib:skin/apply_lit.
#
# A BLAST furnace, matching the item you place it from. It was a plain furnace,
# which meant the thing in your hand and the thing in the world were different
# blocks -- a small mismatch that makes a machine look like the wrong machine.
#
# The common case is a skin that already exists and only needs its `lit` state
# changed, so try that first -- see ra_lib:skin/set_lit for why re-running
# apply_lit for a state change made the block flicker and z-fight. apply_lit is
# only reached when there is genuinely no display to edit: first placement, or
# after a chunk reload cleared it.

data modify storage ra:wires ef.skin.id set value "electric_furnace"
data modify storage ra:wires ef.skin.lit set value "false"
execute if entity @s[tag=ra.wires.ef_lit] run data modify storage ra:wires ef.skin.lit set value "true"

execute store result score #ef.skinned ra.wires.tmp run function ra_lib:skin/set_lit with storage ra:wires ef.skin
execute if score #ef.skinned ra.wires.tmp matches 1.. run return 0

execute if entity @s[tag=ra.wires.ef_lit] run function ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:blast_furnace",id:"electric_furnace",lit:"true"}
execute unless entity @s[tag=ra.wires.ef_lit] run function ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:blast_furnace",id:"electric_furnace",lit:"false"}
