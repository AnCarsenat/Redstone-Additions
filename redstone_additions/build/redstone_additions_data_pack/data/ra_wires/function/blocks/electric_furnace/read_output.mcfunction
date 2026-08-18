# /ra_wires:blocks/electric_furnace/read_output
# Turn the `output` property into an offset in storage ra:wires ef.out.
# Context: as the marker.
#
# ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. The furnace is
# placed with dir 1, horizontal only, so front is always one of the four compass
# directions and back is its opposite.
#
# Offsets rather than caret coordinates because carets depend on the marker's
# Rotation, and a facing score is the thing the rest of the pack already trusts.

execute unless data entity @s data.properties.output run data modify entity @s data.properties.output set value "under"

data modify storage ra:wires ef.out set value {dx:0,dy:-1,dz:0}

execute if data entity @s data.properties{output:"top"} run data modify storage ra:wires ef.out set value {dx:0,dy:1,dz:0}

execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 2 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:-1}
execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 3 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:1}
execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 4 run data modify storage ra:wires ef.out set value {dx:-1,dy:0,dz:0}
execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 5 run data modify storage ra:wires ef.out set value {dx:1,dy:0,dz:0}

execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 2 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:1}
execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 3 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:-1}
execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 4 run data modify storage ra:wires ef.out set value {dx:1,dy:0,dz:0}
execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 5 run data modify storage ra:wires ef.out set value {dx:-1,dy:0,dz:0}

data modify entity @s data.status.output set from entity @s data.properties.output
