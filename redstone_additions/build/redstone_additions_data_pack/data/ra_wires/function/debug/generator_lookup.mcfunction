# /ra_wires:debug/generator_lookup {id}
# Internal: copy the fuel_map entry for $(id), if there is one.

$data modify storage ra:wires dbg.hit set from storage ra:wires fuel_map."$(id)"
