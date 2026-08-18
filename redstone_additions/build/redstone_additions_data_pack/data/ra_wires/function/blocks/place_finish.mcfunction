# /ra_wires:blocks/place_finish
# Internal: initialise a freshly placed RA Wires marker from storage ra:wires spec.
# Context: as the new marker, at its position.
#
# Everything is read straight out of the spec rather than passed as macro
# arguments: a macro line is substituted before its own `execute if` is
# evaluated, so an optional field would break every block that does not have one.

# A block with nothing configurable omits props entirely rather than carrying an
# empty compound, so this is conditional.
execute if data storage ra:wires spec.props run data modify entity @s data.properties set from storage ra:wires spec.props
execute if data storage ra:wires spec.nodedata run data modify entity @s data.data set from storage ra:wires spec.nodedata

tag @s add ra.wires.node

execute if data storage ra:wires spec.fluid run tag @s add ra.wires.fluid_node
execute if data storage ra:wires spec.electric run tag @s add ra.wires.electric_node
# A bridge is deliberately not a node: ra_wires:bridge/tick reaches into the
# networks on either side of it instead of joining one.
execute if data storage ra:wires spec.bridge run tag @s add ra.wires.bridge
execute if data storage ra:wires spec.net run function ra_wires:blocks/place_join with storage ra:wires spec

execute if data storage ra:wires spec{marker:"electric_generator"} run function ra_wires:blocks/electric_generator/refresh_display
execute if data storage ra:wires spec{marker:"electric_furnace"} run function ra_wires:blocks/electric_furnace/refresh_display

function ra_wires:common/update_model_local_and_neighbors
