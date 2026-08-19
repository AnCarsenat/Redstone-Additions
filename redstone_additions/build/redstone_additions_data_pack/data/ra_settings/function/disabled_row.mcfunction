# /ra_settings:disabled_row {b:"electric_furnace"}
# Internal: one disabled block, with the button that re-enables it.
#
# The stored form is just a block name, so the readable label and the action code
# are looked up in the generated blockmap. A name with no entry is still shown --
# raw, and without a button -- because a block removed from the pack while
# disabled would otherwise vanish from this page and stay disabled forever with
# nothing to point at.

data remove storage ra:settings drow
$data modify storage ra:settings drow set from storage ra:settings blockmap[{b:"$(b)"}]

execute if data storage ra:settings drow run function ra_settings:disabled_row_show with storage ra:settings drow
$execute unless data storage ra:settings drow run tellraw @s [{text:"  $(b)",color:"red"},{text:"  (no longer in this pack)",color:"dark_gray"}]

data remove storage ra:settings drow
