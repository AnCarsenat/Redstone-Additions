# /ra_ender:tools/anchor/set_table {table:["A","B","C"]}
# Replace the whole target table of the nearest anchor. Context: as a player.
#
# This is the one the Data Handler points at, because a list is not something its
# per-property rows can edit: you type the list itself. Shorter lists are padded
# with empty rows, so {table:["A","B"]} means signal 1 -> A, signal 2 -> B and
# nothing else wired.

execute unless entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6] run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"No Teleport Anchor within 6 blocks",color:"red"}]

# Parse what was typed before touching the anchor: a macro that fails to parse
# leaves the table absent, and wiping first would have destroyed the old table on a
# typo. Removing it first also stops a stale list from an earlier call being copied.
data remove storage ra:ender table
$data modify storage ra:ender table set value {rows:$(table)}
execute unless data storage ra:ender table.rows run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"Could not read that list — try ",color:"red"},{text:"{table:[\"A\",\"B\"]}",color:"yellow"}]

# Now it is safe to clear the fifteen rows and write the typed ones over the front.
data modify entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.targets set value ["","","","","","","","","","","","","","",""]
data modify storage ra:ender table.i set value 0
function ra_ender:tools/anchor/copy_row

data modify storage ra:ender show set value {}
data modify storage ra:ender show.targets set from entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.targets
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Table set to ",color:"gray"},{nbt:"show.targets",storage:"ra:ender",color:"aqua"}]
