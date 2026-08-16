# /ra_wires:fluid/boiler_cleanup
# Internal: drop the per-boiler working tags.

tag @e[type=marker,tag=ra.wires.boil_src] remove ra.wires.boil_src
tag @e[type=marker,tag=ra.wires.boil_dst] remove ra.wires.boil_dst
