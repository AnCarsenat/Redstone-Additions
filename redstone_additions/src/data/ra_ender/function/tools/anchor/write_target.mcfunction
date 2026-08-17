# /ra_ender:tools/anchor/write_target {i:N,id:"A"}
# Internal: the dynamic-index half of set_target.

$data modify entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.targets[$(i)] set value "$(id)"
