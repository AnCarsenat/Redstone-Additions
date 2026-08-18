# /ra:tools/wrench/menu_at {x,y,z,i}
# Internal: find the remembered block and cycle entry i on it.
#
# The marker is searched for at the stored position rather than trusted to still
# be there: it can have been broken between opening the menu and clicking, and a
# button that silently did nothing would be worse than one that says so.

$execute positioned $(x) $(y) $(z) unless entity @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1] run return run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"That block is gone.",color:"gray"}]

$execute positioned $(x) $(y) $(z) as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] at @s run function ra:tools/wrench/cycle_index {i:$(i)}
