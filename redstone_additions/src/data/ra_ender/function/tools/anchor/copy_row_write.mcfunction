# /ra_ender:tools/anchor/copy_row_write {i:N}
# Internal: write the row at the front of the typed list into slot i.

$data modify entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.targets[$(i)] set from storage ra:ender table.rows[0]
