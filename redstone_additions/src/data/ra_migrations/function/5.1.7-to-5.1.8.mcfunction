# /ra_migrations:5.1.7-to-5.1.8
# Give every entity this pack owns the plain `ra` tag.
#
# 5.1.8 tags everything it summons with `ra` from birth, so that
# `/kill @e[tag=ra]` sweeps up the pack and nothing else. A world built on 5.1.7
# or earlier is full of markers, skins and billboards that predate the tag and
# would be left behind by exactly the command meant to clean them up.
#
# Idempotent: adding a tag an entity already has does nothing.

tag @e[tag=ra.custom_block] add ra
tag @e[tag=ra.multiblock] add ra
tag @e[tag=ra.display] add ra
tag @e[tag=ra.skin] add ra
tag @e[tag=ra.billboard] add ra
tag @e[tag=ra.spawned] add ra
tag @e[tag=ra.node] add ra
tag @e[tag=ra.wires.node] add ra
tag @e[tag=ra.wires.fluid_node] add ra
tag @e[tag=ra.wires.electric_node] add ra
