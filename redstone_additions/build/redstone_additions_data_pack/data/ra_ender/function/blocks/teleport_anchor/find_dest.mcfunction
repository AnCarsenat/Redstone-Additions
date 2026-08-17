# /ra_ender:blocks/teleport_anchor/find_dest {want:"..."}
# Internal: tag the nearest enabled anchor carrying this id.
# Context: as the sending anchor marker, wearing ra.ender.self.

$execute as @e[type=marker,tag=ra.custom_block.teleport_anchor,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{anchor_id:"$(want)"} unless data entity @s data.properties{enabled:0b} run tag @s add ra.ender.tp_dest
