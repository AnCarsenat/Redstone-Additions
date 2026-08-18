# /ra_wires:blocks/electric_furnace/deliver {dx,dy,dz}
# Internal: push one result into the chosen neighbour.
# Context: as the marker, at the block. storage ra:wires ef.hit holds the result.

$execute positioned ~$(dx) ~$(dy) ~$(dz) run function ra_wires:blocks/electric_furnace/deliver_at with storage ra:wires ef.hit
