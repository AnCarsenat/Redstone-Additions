# /ra_ender:blocks/teleport_anchor/match_target
# Internal: tag this anchor if its id is the one being aimed at, first match wins.
# Context: as a candidate anchor marker.

execute if entity @e[type=marker,tag=ra.ender.tp_dest,limit=1] run return 0

execute store result score #ender.candidate ra.temp run data get entity @s data.properties.anchor_id
execute unless score #ender.candidate ra.temp = #ender.target ra.temp run return 0

tag @s add ra.ender.tp_dest
